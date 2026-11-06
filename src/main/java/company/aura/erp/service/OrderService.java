package company.aura.erp.service;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.order.*;
import company.aura.erp.entity.*;
import company.aura.erp.enums.*;
import company.aura.erp.repository.*;
import company.aura.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
    private final OrderPaymentService orderPaymentService;
    private final OrderItemService orderItemService;
    private final OrderShippingService orderShippingService;


    private final OrderInvoiceService orderInvoiceService;

    @Transactional
    public OrderResponse openOrder(OrderCreateRequest request, Long companyId) {

        PartnerEntity partner = validateInitialOrder(request, companyId);

        OrderEntity order = buildInitialOrder(request, companyId);

        OrderEntity savedOrder = orderRepository.save(order);

        OrderShippingEntity savedShipping = orderShippingService.saveInitialShipping(request, savedOrder);

        String customerName = partner != null ? partner.getName() : null;

        if (request.items() == null || request.items().isEmpty()) {
            return OrderResponse.fromEntity(savedOrder, customerName, Collections.emptyList(), Collections.emptyList(), savedShipping);
        }

        List<OrderItemEntity> savedItems = orderItemService.saveInitialItems(request, savedOrder);

        recalculateOrderTotals(savedOrder, savedItems);

        savedOrder = orderRepository.save(savedOrder);

        return OrderResponse.fromEntity(savedOrder, customerName, savedItems, Collections.emptyList(), savedShipping);
    }

    private PartnerEntity validateInitialOrder(OrderCreateRequest request, Long companyId) {
        return partnerRepository.findByIdAndCompanyId(request.partnerId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }

    private boolean isShippingProvided(OrderCreateRequest request) {
        return request.shipping() != null;
    }

    private BigDecimal resolveFreightAmount(OrderShippingRequest shipping) {

        if (shipping != null && shipping.freightAmount() != null) {
            return shipping.freightAmount();
        }
        return BigDecimal.ZERO;
    }

    private OrderEntity buildInitialOrder(OrderCreateRequest request, Long companyId) {
        BigDecimal freightAmount = BigDecimal.ZERO;

        if (isShippingProvided(request)) {
            freightAmount = resolveFreightAmount(request.shipping());
        }

        return OrderEntity.builder()
                .companyId(companyId)
                .partnerId(request.partnerId())
                .status(OrderStatus.OPEN)
                .freightAmount(freightAmount)
                .notes(request.notes())
                .servicesAmount(BigDecimal.ZERO)
                .productsAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(freightAmount)
                .build();
    }


    @Transactional
    public OrderItemResponse addItem(Long orderId, OrderItemCreateRequest request, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        OrderItemEntity item = orderItemService.createItem(request, order);

        recalculateOrderTotals(order);

        return OrderItemResponse.fromEntity(item);
    }

    @Transactional
    public OrderItemResponse updateItem(Long orderId, Long itemId, OrderItemUpdateRequest request, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        OrderItemEntity item = orderItemService.updateOrderItem(order, itemId, request);

        recalculateOrderTotals(order);

        return OrderItemResponse.fromEntity(item);
    }

    @Transactional
    public void removeItem(Long orderId, Long itemId, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        orderItemService.deleteItem(orderId, itemId);

        recalculateOrderTotals(order);
    }

    @Transactional
    public OrderResponse updateOrder(Long orderId, OrderUpdateRequest request, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        if (request.partnerId() != null) {
            partnerRepository.findByIdAndCompanyId(request.partnerId(), companyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
            order.setPartnerId(request.partnerId());
        }

        if (request.notes() != null) {
            order.setNotes(request.notes());
        }

        OrderEntity savedOrder = orderRepository.save(order);

        String customerName = savedOrder.getPartnerId() != null
                ? partnerRepository.findByIdAndCompanyId(savedOrder.getPartnerId(), companyId)
                        .map(PartnerEntity::getName)
                        .orElse(null)
                : null;

        List<OrderItemEntity> items = orderItemService.findOrderItems(order.getId());
        List<OrderInstallmentEntity> installments = orderPaymentService.findInstallments(order.getId());
        OrderShippingEntity shipping = orderShippingService.findByOrderId(order.getId()).orElse(null);

        return OrderResponse.fromEntity(savedOrder, customerName, items, installments, shipping, null);
    }

    @Transactional
    public void deleteShipping(Long orderId, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        orderShippingService.deleteShipping(orderId);
        order.setFreightAmount(BigDecimal.ZERO);
        recalculateOrderTotals(order);
    }

    @Transactional
    public OrderShippingResponse updateShipping(Long orderId, OrderShippingRequest request, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        OrderShippingEntity shipping = orderShippingService.updateShipping(orderId, request);

        if (shipping != null && request.freightAmount() != null) {
            order.setFreightAmount(request.freightAmount());
        } else if (shipping == null) {
            order.setFreightAmount(BigDecimal.ZERO);
        }

        recalculateOrderTotals(order);

        return OrderShippingResponse.fromEntity(shipping);
    }


    public OrderResponse findById(Long orderId, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        String customerName = order.getPartnerId() != null
                ? partnerRepository.findByIdAndCompanyId(order.getPartnerId(), companyId)
                        .map(PartnerEntity::getName)
                        .orElse(null)
                : null;

        List<OrderItemEntity> items = orderItemService.findOrderItems(order.getId());
        List<OrderInstallmentResponse> installments = orderPaymentService.findInstallmentResponses(order.getId());
        OrderShippingEntity shipping = orderShippingService.findByOrderId(order.getId()).orElse(null);
        List<OrderInvoiceResponse> invoices = orderInvoiceService.findInvoicesByOrderId(companyId, order.getId());

        return OrderResponse.fromEntityWithResponses(order, customerName, items, installments, shipping, invoices);
    }

    @Transactional
    public OrderInstallmentResponse payInstallment(Long orderId, Long installmentId, PaymentTransactionRequest request, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);
        OrderInstallmentResponse response = orderPaymentService.payInstallment(order, installmentId, request);
        syncOrderStatusAfterInstallmentChange(order);
        return response;
    }

    @Transactional
    public OrderInstallmentResponse addInstallment(Long orderId, OrderInstallmentCreateRequest request, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);
        OrderInstallmentResponse response = orderPaymentService.addInstallment(order, request);
        syncOrderStatusAfterInstallmentChange(order);
        return response;
    }

    @Transactional
    public OrderInstallmentResponse cancelInstallment(Long orderId, Long installmentId, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);
        OrderInstallmentResponse response = orderPaymentService.cancelInstallment(order, installmentId);
        syncOrderStatusAfterInstallmentChange(order);
        return response;
    }

    private void syncOrderStatusAfterInstallmentChange(OrderEntity order) {
        if (order.getStatus() == OrderStatus.OPEN || order.getStatus() == OrderStatus.CANCELED) {
            return;
        }

        List<OrderInstallmentEntity> activeInstallments = orderPaymentService.findInstallments(order.getId())
                .stream()
                .filter(i -> i.getStatus() != InstallmentStatus.CANCELED)
                .toList();

        if (!activeInstallments.isEmpty() && activeInstallments.stream().allMatch(i -> i.getStatus() == InstallmentStatus.PAID)) {
            order.setStatus(OrderStatus.COMPLETED);
            if (order.getCompletedAt() == null) {
                order.setCompletedAt(LocalDateTime.now());
            }
        } else {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setCompletedAt(null);
        }
        orderRepository.save(order);
    }

    @Transactional
    public OrderResponse closeOrder(Long orderId, OrderCloseRequest request, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        if (isShippingProvided(request)) {
            BigDecimal updatedFreight = resolveFreightAmount(request.shipping());
            order.setFreightAmount(updatedFreight);
        }

        OrderShippingEntity savedShipping = orderShippingService.upsertShippingOnClose(order.getId(), request);

        var items = orderItemService.closeItem(orderId);

        recalculateOrderTotals(order, items);

        var savedInstallments = orderPaymentService.processInstallments(request, order);

        order.setStatus(getOrderFinalStatus(savedInstallments));
        OrderEntity savedOrder = orderRepository.save(order);

        String customerName = savedOrder.getPartnerId() != null
                ? partnerRepository.findByIdAndCompanyId(savedOrder.getPartnerId(), companyId)
                        .map(PartnerEntity::getName)
                        .orElse(null)
                : null;

        return OrderResponse.fromEntity(savedOrder, customerName, items, savedInstallments, savedShipping, null);
    }

    @Transactional
    public InvoiceResponse linkExternalInvoice(Long orderId, OrderExternalInvoiceRequest request, MultipartFile xmlFile, MultipartFile pdfFile, UserPrincipal currentUser) {

        OrderEntity order = findOrder(orderId, currentUser.getCompanyId());

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível vincular nota fiscal a um pedido cancelado.");
        }

        return orderInvoiceService.linkExternalInvoice(order, request, xmlFile, pdfFile, currentUser);
    }

    @Transactional
    public OrderInvoiceResponse linkInvoice(Long orderId, Long invoiceId, LinkOrderInvoice request, UserPrincipal currentUser) {
        OrderEntity order = findOrder(orderId, currentUser.getCompanyId());

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível vincular nota fiscal a um pedido cancelado.");
        }

        return orderInvoiceService.linkInvoice(invoiceId, request, currentUser, order);
    }


    public List<OrderInvoiceResponse> findInvoicesByOrderId(Long orderId, Long companyId) {
        validIfIsCompanyOrder(orderId, companyId);
        return orderInvoiceService.findInvoicesByOrderId(companyId, orderId);
    }



    private static OrderStatus getOrderFinalStatus(List<OrderInstallmentEntity> savedInstallments) {

        boolean paymentCompleted = savedInstallments.stream().map(OrderInstallmentEntity::getStatus).allMatch(InstallmentStatus.PAID::equals);

        if(paymentCompleted) {
            return OrderStatus.COMPLETED;
        }

        return OrderStatus.PENDING_PAYMENT;
    }

    private boolean isShippingProvided(OrderCloseRequest request) {
        return request.shipping() != null && request.shipping().freightAmount() != null;
    }

    private void recalculateOrderTotals(OrderEntity order, List<OrderItemEntity> items) {

        BigDecimal servicesAmount = BigDecimal.ZERO;
        BigDecimal productsAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (OrderItemEntity item : items) {
            BigDecimal itemTotal = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;

            if (item.getItemType() == CatalogItemType.SERVICE) {
                servicesAmount = servicesAmount.add(itemTotal);
            } else {
                productsAmount = productsAmount.add(itemTotal);
            }

            if (item.getDiscountAmount() != null) {
                discountAmount = discountAmount.add(item.getDiscountAmount());
            }
        }

        BigDecimal freight = order.getFreightAmount() != null ? order.getFreightAmount() : BigDecimal.ZERO;
        BigDecimal total = servicesAmount.add(productsAmount).add(freight);

        order.setServicesAmount(servicesAmount);
        order.setProductsAmount(productsAmount);
        order.setDiscountAmount(discountAmount);

        order.setTotalAmount(total);
    }

    private OrderEntity findOrder(Long orderId, Long companyId) {
        return orderRepository.findByIdAndCompanyId(orderId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado com ID: " + orderId));
    }

    private void validIfIsCompanyOrder(Long orderId, Long companyId) {
        if (orderRepository.existsByIdAndCompanyId(orderId, companyId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado com ID: " + orderId);
        }
    }

    private void recalculateOrderTotals(OrderEntity order) {
        List<OrderItemEntity> allItems = orderItemService.findOrderItems(order.getId());
        recalculateOrderTotals(order, allItems);
        orderRepository.save(order);
    }

    private void validateIfOrderIsEditable(OrderEntity order) {
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O pedido não pode ser alterado pois não está aberto.");
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listSummarized(String search, Long companyId, Pageable pageable) {
        Pageable effectivePageable = pageable;
        if (pageable != null && pageable.getSort().isSorted()) {
            Sort mappedSort = Sort.by(
                    pageable.getSort().stream().map(order -> {
                        String prop = order.getProperty();
                        if ("customerName".equalsIgnoreCase(prop) || "name".equalsIgnoreCase(prop)) {
                            return new Sort.Order(order.getDirection(), "p.name");
                        }
                        if ("customerDocument".equalsIgnoreCase(prop) || "document".equalsIgnoreCase(prop)) {
                            return new Sort.Order(order.getDirection(), "p.document");
                        }
                        if ("id".equalsIgnoreCase(prop)) {
                            return new Sort.Order(order.getDirection(), "o.id");
                        }
                        if ("totalAmount".equalsIgnoreCase(prop)) {
                            return new Sort.Order(order.getDirection(), "o.totalAmount");
                        }
                        if ("status".equalsIgnoreCase(prop)) {
                            return new Sort.Order(order.getDirection(), "o.status");
                        }
                        if ("createdAt".equalsIgnoreCase(prop)) {
                            return new Sort.Order(order.getDirection(), "o.createdAt");
                        }
                        return order;
                    }).toList()
            );
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
        } else if (pageable != null) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "o.createdAt"));
        }

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return orderRepository.searchSummarized(companyId, searchTerm, effectivePageable);
    }
}
