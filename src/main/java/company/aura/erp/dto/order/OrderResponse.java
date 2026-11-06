package company.aura.erp.dto.order;

import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.entity.OrderShippingEntity;
import company.aura.erp.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record OrderResponse(
        Long id,
        Long companyId,
        Long partnerId,
        String customerName,
        Long nfseInvoiceId,
        Long nfeInvoiceId,
        OrderStatus status,
        BigDecimal servicesAmount,
        BigDecimal productsAmount,
        BigDecimal freightAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String notes,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items,
        List<OrderInstallmentResponse> installments,
        OrderShippingResponse shipping
) {
    public OrderResponse(
            Long id,
            Long companyId,
            Long partnerId,
            Long nfseInvoiceId,
            Long nfeInvoiceId,
            OrderStatus status,
            BigDecimal servicesAmount,
            BigDecimal productsAmount,
            BigDecimal freightAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String notes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items,
            List<OrderInstallmentResponse> installments
    ) {
        this(id, companyId, partnerId, null, nfseInvoiceId, nfeInvoiceId, status,
                servicesAmount, productsAmount, freightAmount, discountAmount,
                totalAmount, notes, completedAt, createdAt, updatedAt, items, installments, null);
    }

    public OrderResponse(
            Long id,
            Long companyId,
            Long partnerId,
            String customerName,
            Long nfseInvoiceId,
            Long nfeInvoiceId,
            OrderStatus status,
            BigDecimal servicesAmount,
            BigDecimal productsAmount,
            BigDecimal freightAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String notes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items,
            List<OrderInstallmentResponse> installments
    ) {
        this(id, companyId, partnerId, customerName, nfseInvoiceId, nfeInvoiceId, status,
                servicesAmount, productsAmount, freightAmount, discountAmount,
                totalAmount, notes, completedAt, createdAt, updatedAt, items, installments, null);
    }

    public OrderResponse(
            Long id,
            Long companyId,
            Long partnerId,
            Long nfseInvoiceId,
            Long nfeInvoiceId,
            OrderStatus status,
            BigDecimal servicesAmount,
            BigDecimal productsAmount,
            BigDecimal freightAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String notes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items,
            List<OrderInstallmentResponse> installments,
            OrderShippingResponse shipping
    ) {
        this(id, companyId, partnerId, null, nfseInvoiceId, nfeInvoiceId, status,
                servicesAmount, productsAmount, freightAmount, discountAmount,
                totalAmount, notes, completedAt, createdAt, updatedAt, items, installments, shipping);
    }

    public static OrderResponse fromEntity(
            OrderEntity order,
            List<OrderItemEntity> items,
            List<OrderInstallmentEntity> installments
    ) {
        return fromEntity(order, null, items, installments, null);
    }

    public static OrderResponse fromEntity(
            OrderEntity order,
            List<OrderItemEntity> items,
            List<OrderInstallmentEntity> installments,
            OrderShippingEntity shipping
    ) {
        return fromEntity(order, null, items, installments, shipping);
    }

    public static OrderResponse fromEntity(
            OrderEntity order,
            String customerName,
            List<OrderItemEntity> items,
            List<OrderInstallmentEntity> installments,
            OrderShippingEntity shipping
    ) {
        List<OrderItemResponse> itemResponses = items != null
                ? items.stream().map(OrderItemResponse::fromEntity).toList()
                : Collections.emptyList();

        List<OrderInstallmentResponse> installmentResponses = installments != null
                ? installments.stream().map(OrderInstallmentResponse::fromEntity).toList()
                : Collections.emptyList();

        OrderShippingResponse shippingResponse = OrderShippingResponse.fromEntity(shipping);

        return new OrderResponse(
                order.getId(),
                order.getCompanyId(),
                order.getPartnerId(),
                customerName,
                order.getNfseInvoiceId(),
                order.getNfeInvoiceId(),
                order.getStatus(),
                order.getServicesAmount(),
                order.getProductsAmount(),
                order.getFreightAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getCompletedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses,
                installmentResponses,
                shippingResponse
        );
    }

    public static OrderResponse fromEntityWithResponses(
            OrderEntity order,
            List<OrderItemEntity> items,
            List<OrderInstallmentResponse> installments,
            OrderShippingEntity shipping
    ) {
        return fromEntityWithResponses(order, null, items, installments, shipping);
    }

    public static OrderResponse fromEntityWithResponses(
            OrderEntity order,
            String customerName,
            List<OrderItemEntity> items,
            List<OrderInstallmentResponse> installments,
            OrderShippingEntity shipping
    ) {
        List<OrderItemResponse> itemResponses = items != null
                ? items.stream().map(OrderItemResponse::fromEntity).toList()
                : Collections.emptyList();

        OrderShippingResponse shippingResponse = OrderShippingResponse.fromEntity(shipping);

        return new OrderResponse(
                order.getId(),
                order.getCompanyId(),
                order.getPartnerId(),
                customerName,
                order.getNfseInvoiceId(),
                order.getNfeInvoiceId(),
                order.getStatus(),
                order.getServicesAmount(),
                order.getProductsAmount(),
                order.getFreightAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getCompletedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses,
                installments != null ? installments : Collections.emptyList(),
                shippingResponse
        );
    }
}
