package company.aura.erp.dto.order;

import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record OrderResponse(
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
    public static OrderResponse fromEntity(
            OrderEntity order,
            List<OrderItemEntity> items,
            List<OrderInstallmentEntity> installments
    ) {
        List<OrderItemResponse> itemResponses = items != null
                ? items.stream().map(OrderItemResponse::fromEntity).toList()
                : Collections.emptyList();

        List<OrderInstallmentResponse> installmentResponses = installments != null
                ? installments.stream().map(OrderInstallmentResponse::fromEntity).toList()
                : Collections.emptyList();

        return new OrderResponse(
                order.getId(),
                order.getCompanyId(),
                order.getPartnerId(),
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
                installmentResponses
        );
    }
}
