package company.aura.erp.mapper;

import company.aura.erp.dto.catalog.CatalogItemResponse;
import company.aura.erp.dto.order.OrderItemCreateRequest;
import company.aura.erp.dto.order.OrderItemResponse;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.modules.invoice.domain.InvoiceItemData;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

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

    public static OrderItemCreateRequest toOrderItemCreateRequest(InvoiceItemData xmlItem, CatalogItemResponse catalogItem) {
        if (xmlItem == null || catalogItem == null) {
            return null;
        }

        String rawDescription = StringUtils.isNotBlank(xmlItem.getDescription())
                ? xmlItem.getDescription().trim()
                : catalogItem.description();
        String description = rawDescription != null && rawDescription.length() > 255
                ? rawDescription.substring(0, 255)
                : rawDescription;

        BigDecimal unitPrice = xmlItem.getUnitPrice() != null && xmlItem.getUnitPrice().compareTo(BigDecimal.ZERO) > 0
                ? xmlItem.getUnitPrice()
                : catalogItem.unitPrice();

        BigDecimal discountAmount = xmlItem.getDiscountAmount() != null && xmlItem.getDiscountAmount().compareTo(BigDecimal.ZERO) >= 0
                ? xmlItem.getDiscountAmount()
                : BigDecimal.ZERO;

        return OrderItemCreateRequest.builder()
                .catalogItemId(catalogItem.id())
                .description(description)
                .quantity(xmlItem.getQuantity())
                .unitPrice(unitPrice)
                .discountAmount(discountAmount)
                .build();
    }
}
