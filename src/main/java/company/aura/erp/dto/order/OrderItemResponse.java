package company.aura.erp.dto.order;

import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.enums.CatalogItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemResponse(
        Long id,
        Long orderId,
        Long catalogItemId,
        CatalogItemType itemType,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal totalPrice,
        Long professionalId,
        Boolean stockDeducted,
        LocalDateTime createdAt
) {
    public static OrderItemResponse fromEntity(OrderItemEntity entity) {
        return new OrderItemResponse(
                entity.getId(),
                entity.getOrderId(),
                entity.getCatalogItemId(),
                entity.getItemType(),
                entity.getDescription(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getDiscountAmount(),
                entity.getTotalPrice(),
                entity.getProfessionalId(),
                entity.getStockDeducted(),
                entity.getCreatedAt()
        );
    }
}
