package company.aura.erp.service;

import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.order.*;
import company.aura.erp.entity.*;
import company.aura.erp.enums.*;
import company.aura.erp.mapper.OrderItemMapper;
import company.aura.erp.mapper.OrderMapper;
import company.aura.erp.mapper.OrderShippingMapper;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.PartnerRepository;
import company.aura.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
    private final ReceivedService receivedService;
    private final OrderItemService orderItemService;
    private final OrderShippingService orderShippingService;
    private final OrderInvoiceService orderInvoiceService;

    @Transactional
    public OrderResponse openOrder(OrderCreateRequest request, Long companyId) {
        log.info("openOrder() - START - companyId={}, partnerId={}", companyId, request.getPartnerId());

        PartnerEntity partner = findPartner(request.getPartnerId(), companyId);

        OrderEntity order = buildInitialOrder(request, companyId);

        OrderEntity savedOrder = orderRepository.save(order);

        OrderShippingEntity savedShipping = orderShippingService.saveInitialShipping(request, savedOrder);

        String customerName = partner.getName();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            OrderResponse response = OrderMapper.fromEntity(savedOrder, customerName, Collections.emptyList(), Collections.emptyList(), savedShipping);
            log.info("openOrder() - END - orderId={}, companyId={}", response.getId(), companyId);
            return response;
        }

        List<OrderItemEntity> savedItems = orderItemService.saveInitialItems(request, savedOrder);

        recalculateOrderTotals(savedOrder, savedItems);

        savedOrder = orderRepository.save(savedOrder);

        OrderResponse response = OrderMapper.fromEntity(savedOrder, customerName, savedItems, Collections.emptyList(), savedShipping);
        log.info("openOrder() - END - orderId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    private PartnerEntity findPartner(Long partnerId, Long companyId) {
        return partnerRepository.findByIdAndCompanyId(partnerId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }

    private boolean isShippingProvided(OrderCreateRequest request) {
        return request.getShipping() != null;
    }

    private BigDecimal resolveFreightAmount(OrderShippingRequest shipping) {
        if (shipping != null && shipping.getFreightAmount() != null) {
            return shipping.getFreightAmount();
        }
        return BigDecimal.ZERO;
    }

    private OrderEntity buildInitialOrder(OrderCreateRequest request, Long companyId) {
        BigDecimal freightAmount = BigDecimal.ZERO;

        if (isShippingProvided(request)) {
            freightAmount = resolveFreightAmount(request.getShipping());
        }

        return OrderEntity.builder()
                .companyId(companyId)
                .partnerId(request.getPartnerId())
                .status(OrderStatus.OPEN)
                .freightAmount(freightAmount)
                .notes(request.getNotes())
                .servicesAmount(BigDecimal.ZERO)
                .productsAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(freightAmount)
                .build();
    }

    @Transactional
    public OrderItemResponse addItem(Long orderId, OrderItemCreateRequest request, Long companyId) {
        log.info("addItem() - START - orderId={}, companyId={}", orderId, companyId);

        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        OrderItemEntity item = orderItemService.createItem(request, order);

        recalculateOrderTotals(order);

        OrderItemResponse response = OrderItemMapper.fromEntity(item);
        log.info("addItem() - END - orderId={}, itemId={}, companyId={}", orderId, response.getId(), companyId);
        return response;
    }

    @Transactional
    public OrderItemResponse updateItem(Long orderId, Long itemId, OrderItemUpdateRequest request, Long companyId) {
        log.info("updateItem() - START - orderId={}, itemId={}, companyId={}", orderId, itemId, companyId);

        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        OrderItemEntity item = orderItemService.updateOrderItem(order, itemId, request);

        recalculateOrderTotals(order);

        OrderItemResponse response = OrderItemMapper.fromEntity(item);
        log.info("updateItem() - END - orderId={}, itemId={}, companyId={}", orderId, response.getId(), companyId);
        return response;
    }

    @Transactional
    public void removeItem(Long orderId, Long itemId, Long companyId) {
        log.info("removeItem() - START - orderId={}, itemId={}, companyId={}", orderId, itemId, companyId);

        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        orderItemService.deleteItem(orderId, itemId);

        recalculateOrderTotals(order);

        log.info("removeItem() - END - orderId={}, itemId={}, companyId={}", orderId, itemId, companyId);
    }

    @Transactional
    public OrderResponse updateOrder(Long orderId, OrderUpdateRequest request, Long companyId) {
        log.info("updateOrder() - START - orderId={}, companyId={}", orderId, companyId);

        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        PartnerEntity partner =  findPartner(request.getPartnerId(), companyId);

        order.setPartnerId(request.getPartnerId());
        order.setNotes(request.getNotes());

        OrderEntity savedOrder = orderRepository.save(order);

        List<OrderItemEntity> items = orderItemService.findOrderItems(order.getId());
        OrderShippingEntity shipping = orderShippingService.findByOrderId(order.getId()).orElse(null);

        OrderResponse response = OrderMapper.fromEntity(savedOrder, partner.getName(), items, null, shipping, null);
        log.info("updateOrder() - END - orderId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public void deleteShipping(Long orderId, Long companyId) {
        log.info("deleteShipping() - START - orderId={}, companyId={}", orderId, companyId);

        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        orderShippingService.deleteShipping(orderId);
        order.setFreightAmount(BigDecimal.ZERO);
        recalculateOrderTotals(order);

        log.info("deleteShipping() - END - orderId={}, companyId={}", orderId, companyId);
    }

    @Transactional
    public OrderShippingResponse updateShipping(Long orderId, OrderShippingRequest request, Long companyId) {
        log.info("updateShipping() - START - orderId={}, companyId={}", orderId, companyId);

        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        OrderShippingEntity shipping = orderShippingService.updateShipping(orderId, request);

        if (shipping != null && request.getFreightAmount() != null) {
            order.setFreightAmount(request.getFreightAmount());
        } else if (shipping == null) {
            order.setFreightAmount(BigDecimal.ZERO);
        }

        recalculateOrderTotals(order);

        OrderShippingResponse response = OrderShippingMapper.fromEntity(shipping);
        log.info("updateShipping() - END - orderId={}, companyId={}", orderId, companyId);
        return response;
    }

    public OrderResponse findById(Long orderId, Long companyId) {
        log.info("findById() - START - orderId={}, companyId={}", orderId, companyId);

        OrderEntity order = findOrder(orderId, companyId);

        String customerName = order.getPartnerId() != null
                ? partnerRepository.findNameByIdAndCompanyId(order.getPartnerId(), companyId)
                        .orElse(null)
                : null;

        List<OrderItemEntity> items = orderItemService.findOrderItems(order.getId());
        List<ReceivableResponse> receivables = receivedService.findReceivableResponses(order.getId());
        OrderShippingEntity shipping = orderShippingService.findByOrderId(order.getId()).orElse(null);
        List<OrderInvoiceResponse> invoices = orderInvoiceService.findInvoicesByOrderId(order.getId(), companyId);

        OrderResponse response = OrderMapper.fromEntityWithResponses(order, customerName, items, receivables, shipping, invoices);
        log.info("findById() - END - orderId={}, companyId={}", response.getId(), companyId);
        return response;
    }



    @Transactional
    public OrderResponse closeOrder(Long orderId, OrderCloseRequest request, Long companyId) {
        log.info("closeOrder() - START - orderId={}, companyId={}", orderId, companyId);
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        List<ReceivableRequest> requestReceivables = (request != null && request.getReceivables() != null)
                ? request.getReceivables()
                : Collections.emptyList();

        validateOrderPayment(requestReceivables, order);

        if (isShippingProvided(request)) {
            BigDecimal updatedFreight = resolveFreightAmount(request.getShipping());
            order.setFreightAmount(updatedFreight);
        }

        OrderShippingEntity savedShipping = orderShippingService.upsertShippingOnClose(order.getId(), request);

        var items = orderItemService.closeItem(orderId);

        recalculateOrderTotals(order, items);

        var receivablesSanitized = requestReceivables.stream()
                .peek(e -> e.setOriginType(ReceivableOriginType.ORDER))
                .peek(e -> e.setPartnerId(order.getPartnerId()))
                .peek(e -> e.setOrderId(orderId))
                .toList();

        if (!receivablesSanitized.isEmpty()) {
            receivedService.processReceivables(receivablesSanitized, companyId);
        }

        order.setStatus(OrderStatus.COMPLETED);
        OrderEntity savedOrder = orderRepository.save(order);

        String customerName = savedOrder.getPartnerId() != null
                ? partnerRepository.findNameByIdAndCompanyId(savedOrder.getPartnerId(), companyId)
                        .orElse(null)
                : null;

        var savedReceivables = receivedService.findReceivableResponses(orderId);

        OrderResponse response = OrderMapper.fromEntity(savedOrder, customerName, items, savedReceivables, savedShipping, null);
        log.info("closeOrder() - END - orderId={}, companyId={}, status={}", response.getId(), companyId, savedOrder.getStatus());
        return response;
    }

    private void validateOrderPayment(List<ReceivableRequest> requestReceivables, OrderEntity order) {
        List<ReceivableResponse> existingReceivables = receivedService.findReceivableResponses(order.getId());
        BigDecimal existingReceivablesTotal = (existingReceivables != null ? existingReceivables : Collections.<ReceivableResponse>emptyList())
                .stream()
                .filter(r -> r.getStatus() != ReceivableStatus.CANCELED)
                .map(ReceivableResponse::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal requestReceivablesTotal = requestReceivables.stream()
                .map(ReceivableRequest::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReceivables = existingReceivablesTotal.add(requestReceivablesTotal);
        BigDecimal expectedTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;

        if (totalReceivables.compareTo(expectedTotal) != 0) {
            BigDecimal receivablesFormatted = totalReceivables.setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalOrderFormatted = expectedTotal.setScale(2, RoundingMode.HALF_UP);

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("A soma das parcelas (R$ %s) não confere com o valor total do pedido (R$ %s).",
                            receivablesFormatted, totalOrderFormatted)
            );
        }
    }

    @Transactional
    public InvoiceResponse linkExternalInvoice(Long orderId, OrderExternalInvoiceRequest request, MultipartFile xmlFile, MultipartFile pdfFile, UserPrincipal currentUser) {
        log.info("linkExternalInvoice() - START - orderId={}, userId={}, companyId={}", orderId, currentUser != null ? currentUser.getId() : null, currentUser != null ? currentUser.getCompanyId() : null);

        OrderEntity order = findOrder(orderId, currentUser.getCompanyId());

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível vincular nota fiscal a um pedido cancelado.");
        }

        InvoiceResponse response = orderInvoiceService.linkExternalInvoice(order, request, xmlFile, pdfFile, currentUser);
        log.info("linkExternalInvoice() - END - orderId={}, invoiceId={}", orderId, response != null ? response.getId() : null);
        return response;
    }

    @Transactional
    public OrderInvoiceResponse linkInvoice(Long orderId, Long invoiceId, LinkOrderInvoice request, UserPrincipal currentUser) {
        log.info("linkInvoice() - START - orderId={}, invoiceId={}, userId={}, companyId={}", orderId, invoiceId, currentUser != null ? currentUser.getId() : null, currentUser != null ? currentUser.getCompanyId() : null);

        OrderEntity order = findOrder(orderId, currentUser.getCompanyId());

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível vincular nota fiscal a um pedido cancelado.");
        }

        OrderInvoiceResponse response = orderInvoiceService.linkInvoice(invoiceId, request, currentUser, order);
        log.info("linkInvoice() - END - orderId={}, invoiceId={}", orderId, invoiceId);
        return response;
    }

    @Transactional
    public void unlinkInvoice(Long orderId, Long invoiceId, Long companyId) {
        log.info("unlinkInvoice() - START - orderId={}, invoiceId={}, companyId={}", orderId, invoiceId, companyId);

        OrderEntity order = findOrder(orderId, companyId);

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível desvincular nota fiscal de um pedido cancelado.");
        }

        orderInvoiceService.unlinkInvoice(order.getId(), invoiceId, companyId);
        log.info("unlinkInvoice() - END - orderId={}, invoiceId={}, companyId={}", orderId, invoiceId, companyId);
    }

    public List<OrderInvoiceResponse> findInvoicesByOrderId(Long orderId, Long companyId) {
        log.info("findInvoicesByOrderId() - START - orderId={}, companyId={}", orderId, companyId);
        validIfIsCompanyOrder(orderId, companyId);
        List<OrderInvoiceResponse> invoices = orderInvoiceService.findInvoicesByOrderId(companyId, orderId);
        log.info("findInvoicesByOrderId() - END - orderId={}, companyId={}, totalInvoices={}", orderId, companyId, invoices.size());
        return invoices;
    }


    private boolean isShippingProvided(OrderCloseRequest request) {
        return request != null && request.getShipping() != null && request.getShipping().getFreightAmount() != null;
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
        if (!orderRepository.existsByIdAndCompanyId(orderId, companyId)) {
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
    public Page<OrderSummaryResponse> listSummarized(
            String search,
            LocalDate startDate,
            LocalDate endDate,
            List<OrderStatus> statuses,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listSummarized() - START - companyId={}, search={}, startDate={}, endDate={}, statuses={}", companyId, search, startDate, endDate, statuses);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        boolean hasDateRange = (startDate != null && endDate != null);
        boolean hasStartDateOnly = (startDate != null && endDate == null);
        boolean hasEndDateOnly = (startDate == null && endDate != null);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (hasDateRange) {
            if (startDate.isAfter(endDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
            }
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atTime(LocalTime.MAX);
        } else if (hasStartDateOnly) {
            startDateTime = startDate.atStartOfDay();
        } else if (hasEndDateOnly) {
            endDateTime = endDate.atTime(LocalTime.MAX);
        }

        boolean hasStatuses = (statuses != null && !statuses.isEmpty());
        List<OrderStatus> effectiveStatuses = hasStatuses ? statuses : List.of(OrderStatus.OPEN);

        Page<OrderSummaryResponse> page = orderRepository.searchSummarized(
                companyId,
                searchTerm,
                hasDateRange,
                hasStartDateOnly,
                hasEndDateOnly,
                startDateTime,
                endDateTime,
                hasStatuses,
                effectiveStatuses,
                pageable
        );

        log.info("listSummarized() - END - companyId={}, totalElements={}, totalPages={}", companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listSummarized(String search, Long companyId, Pageable pageable) {
        log.info("listSummarized() - START - companyId={}, search={}", companyId, search);
        Page<OrderSummaryResponse> page = listSummarized(search, null, null, null, companyId, pageable);
        log.info("listSummarized() - END - companyId={}, totalElements={}, totalPages={}", companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }
}
