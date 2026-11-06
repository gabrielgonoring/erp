package company.aura.erp.mapper;

import company.aura.erp.dto.order.OrderShippingResponse;
import company.aura.erp.entity.OrderShippingEntity;

public class OrderShippingMapper {

    public static OrderShippingResponse fromEntity(OrderShippingEntity entity) {
        if (entity == null) {
            return null;
        }
        return OrderShippingResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .carrierId(entity.getCarrierId())
                .freightMode(entity.getFreightMode())
                .vehiclePlate(entity.getVehiclePlate())
                .vehicleState(entity.getVehicleState())
                .rntrcCode(entity.getRntrcCode())
                .packageQuantity(entity.getPackageQuantity())
                .packageSpecies(entity.getPackageSpecies())
                .packageBrand(entity.getPackageBrand())
                .packageNumbering(entity.getPackageNumbering())
                .grossWeightKg(entity.getGrossWeightKg())
                .netWeightKg(entity.getNetWeightKg())
                .trackingCode(entity.getTrackingCode())
                .shippingNotes(entity.getShippingNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
