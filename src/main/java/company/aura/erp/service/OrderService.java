package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.*;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
    private final OrderPaymentService orderPaymentService;
    private final OrderItemService orderItemService;
    private final OrderShippingService orderShippingService;

    @Transactional
    public OrderResponse openOrder(OrderCreateRequest request, Long companyId) {

        validateInitialOrder(request, companyId);

        OrderEntity order = buildInitialOrder(request, companyId);

        OrderEntity savedOrder = orderRepository.save(order);

        OrderShippingEntity savedShipping = orderShippingService.saveInitialShipping(request, savedOrder);

        if (request.items() == null || request.items().isEmpty()) {
            return OrderResponse.fromEntity(savedOrder, Collections.emptyList(), Collections.emptyList(), savedShipping);
        }

        List<OrderItemEntity> savedItems = orderItemService.saveInitialItems(request, savedOrder);

        recalculateOrderTotals(savedOrder, savedItems);

        savedOrder = orderRepository.save(savedOrder);

        return OrderResponse.fromEntity(savedOrder, savedItems, Collections.emptyList(), savedShipping);
    }

    private void validateInitialOrder(OrderCreateRequest request, Long companyId) {
        partnerRepository.findByIdAndCompanyId(request.partnerId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }

    private boolean isShippingProvided(OrderCreateRequest request) {
        return request.shipping() == null;
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

        var updatedItem = orderItemService.updateOrderItem(orderId, itemId, request);

        recalculateOrderTotals(order);

        return OrderItemResponse.fromEntity(updatedItem);
    }

    @Transactional
    public void removeItem(Long orderId, Long itemId, Long companyId) {
        OrderEntity order = findOrder(orderId, companyId);

        validateIfOrderIsEditable(order);

        orderItemService.deleteItem(orderId, itemId);

        recalculateOrderTotals(order);
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

        order.setStatus(OrderStatus.PENDING_PAYMENT);
        OrderEntity savedOrder = orderRepository.save(order);

        return OrderResponse.fromEntity(savedOrder, items, savedInstallments, savedShipping);
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

    private void recalculateOrderTotals(OrderEntity order) {
        List<OrderItemEntity> allItems = orderItemService.findOrderItems(order.getId());
        recalculateOrderTotals(order, allItems);
        orderRepository.save(order);
    }

    private void validateIfOrderIsEditable(OrderEntity order) {
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível adicionar itens a um pedido fechado ou cancelado.");
        }
    }
}
