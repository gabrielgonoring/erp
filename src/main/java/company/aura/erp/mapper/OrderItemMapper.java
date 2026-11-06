package company.aura.erp.mapper;

import company.aura.erp.dto.order.OrderItemResponse;
import company.aura.erp.entity.OrderItemEntity;

public class OrderItemMapper {

    public static OrderItemResponse fromEntity(OrderItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return OrderItemResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .catalogItemId(entity.getCatalogItemId())
                .itemType(entity.getItemType())
                .description(entity.getDescription())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .discountAmount(entity.getDiscountAmount())
                .totalPrice(entity.getTotalPrice())
                .professionalId(entity.getProfessionalId())
                .stockDeducted(entity.getStockDeducted())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
