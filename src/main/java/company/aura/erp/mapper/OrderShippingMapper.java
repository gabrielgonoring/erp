package company.aura.erp.mapper;

import company.aura.erp.dto.order.OrderShippingRequest;
import company.aura.erp.dto.order.OrderShippingResponse;
import company.aura.erp.entity.OrderShippingEntity;
import company.aura.erp.modules.invoice.domain.InvoiceFreightData;
import company.aura.erp.modules.invoice.domain.InvoiceVolumeData;
import company.aura.erp.modules.invoice.domain.ParsedInvoiceXml;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

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

    public static OrderShippingRequest toOrderShippingRequest(ParsedInvoiceXml parsedXml, Long carrierId) {
        if (parsedXml == null) {
            return null;
        }

        InvoiceFreightData freight = parsedXml.getFreight();
        BigDecimal freightAmount = parsedXml.getTotals() != null && parsedXml.getTotals().getFreightAmount() != null
                ? parsedXml.getTotals().getFreightAmount()
                : BigDecimal.ZERO;

        boolean hasFreightDetails = freight != null && (
                (freight.getFreightModeCode() != null && freight.getFreightModeCode() != 9)
                        || carrierId != null
                        || StringUtils.isNotBlank(freight.getVehiclePlate())
                        || (freight.getVolumes() != null && !freight.getVolumes().isEmpty())
        );

        if (!hasFreightDetails && freightAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        InvoiceVolumeData firstVolume = (freight != null && freight.getVolumes() != null && !freight.getVolumes().isEmpty())
                ? freight.getVolumes().get(0)
                : null;

        Integer packageQuantity = firstVolume != null && firstVolume.getQuantity() != null && firstVolume.getQuantity() >= 1
                ? firstVolume.getQuantity()
                : 1;

        return OrderShippingRequest.builder()
                .freightMode(freight != null && freight.getFreightModeCode() != null ? freight.getFreightModeCode() : 9)
                .carrierId(carrierId)
                .vehiclePlate(freight != null ? truncate(trimToNull(freight.getVehiclePlate()), 8) : null)
                .vehicleState(freight != null ? truncate(trimToNull(freight.getVehicleState()), 2) : null)
                .rntrcCode(freight != null ? truncate(trimToNull(freight.getRntrcCode()), 20) : null)
                .packageQuantity(packageQuantity)
                .packageSpecies(firstVolume != null ? truncate(trimToNull(firstVolume.getSpecies()), 50) : null)
                .packageBrand(firstVolume != null ? truncate(trimToNull(firstVolume.getBrand()), 50) : null)
                .packageNumbering(firstVolume != null ? truncate(trimToNull(firstVolume.getNumbering()), 50) : null)
                .grossWeightKg(firstVolume != null ? normalizeNonNegative(firstVolume.getGrossWeightKg()) : BigDecimal.ZERO)
                .netWeightKg(firstVolume != null ? normalizeNonNegative(firstVolume.getNetWeightKg()) : BigDecimal.ZERO)
                .freightAmount(normalizeNonNegative(freightAmount))
                .build();
    }

    private static BigDecimal normalizeNonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private static String trimToNull(String value) {
        return StringUtils.isNotBlank(value) ? value.trim() : null;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
