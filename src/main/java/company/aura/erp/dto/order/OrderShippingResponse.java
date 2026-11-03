package company.aura.erp.dto.order;

import company.aura.erp.entity.OrderShippingEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderShippingResponse(
        Long id,
        Long orderId,
        Long carrierId,
        Integer freightMode,
        String vehiclePlate,
        String vehicleState,
        String rntrcCode,
        Integer packageQuantity,
        String packageSpecies,
        String packageBrand,
        String packageNumbering,
        BigDecimal grossWeightKg,
        BigDecimal netWeightKg,
        String trackingCode,
        String shippingNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderShippingResponse fromEntity(OrderShippingEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderShippingResponse(
                entity.getId(),
                entity.getOrderId(),
                entity.getCarrierId(),
                entity.getFreightMode(),
                entity.getVehiclePlate(),
                entity.getVehicleState(),
                entity.getRntrcCode(),
                entity.getPackageQuantity(),
                entity.getPackageSpecies(),
                entity.getPackageBrand(),
                entity.getPackageNumbering(),
                entity.getGrossWeightKg(),
                entity.getNetWeightKg(),
                entity.getTrackingCode(),
                entity.getShippingNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
