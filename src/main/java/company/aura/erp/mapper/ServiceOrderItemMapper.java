package company.aura.erp.mapper;

import company.aura.erp.dto.serviceorder.ServiceOrderItemCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderItemResponse;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.ServiceOrderItemStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

public class ServiceOrderItemMapper {

    public static ServiceOrderItemEntity toEntity(ServiceOrderItemCreateRequest itemRequest, ServiceOrderEntity serviceOrder, CatalogItemEntity catalogItemEntity){
        BigDecimal unitPrice = itemRequest.getUnitPrice() != null ? itemRequest.getUnitPrice() : catalogItemEntity.getUnitPrice();

        ServiceOrderItemStatus initialStatus = resolveItemStatus(itemRequest.getStatus());

        BigDecimal discount = itemRequest.getDiscountAmount() != null ? itemRequest.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalPrice = calculateTotalPrice(itemRequest.getQuantity(), unitPrice, discount);

        ServiceOrderItemEntity item = ServiceOrderItemEntity.builder()
                .serviceOrderId(serviceOrder.getId())
                .catalogItemId(itemRequest.getCatalogItemId())
                .itemType(catalogItemEntity.getType())
                .status(initialStatus)
                .description(itemRequest.getDescription())
                .quantity(itemRequest.getQuantity())
                .unitPrice(unitPrice)
                .discountAmount(discount)
                .totalPrice(totalPrice)
                .professionalId(itemRequest.getProfessionalId())
                .stockReserved(Boolean.TRUE.equals(itemRequest.getStockReserved()))
                .build();

        return item;
    }

    public static ServiceOrderItemResponse fromEntity(ServiceOrderItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return ServiceOrderItemResponse.builder()
                .id(entity.getId())
                .serviceOrderId(entity.getServiceOrderId())
                .catalogItemId(entity.getCatalogItemId())
                .itemType(entity.getItemType())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .discountAmount(entity.getDiscountAmount())
                .totalPrice(entity.getTotalPrice())
                .professionalId(entity.getProfessionalId())
                .stockReserved(entity.getStockReserved())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static ServiceOrderItemStatus resolveItemStatus(ServiceOrderItemStatus requestedStatus) {
        if (requestedStatus == null) {
            return ServiceOrderItemStatus.PENDING;
        }
        if (requestedStatus == ServiceOrderItemStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Um item não pode ser adicionado ou criado à ordem de serviço diretamente como cancelado.");
        }
        return requestedStatus;
    }


    public static BigDecimal calculateTotalPrice(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount) {
        BigDecimal totalPrice = quantity.multiply(unitPrice).subtract(discount);
        return totalPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : totalPrice;
    }
}
