package company.aura.erp.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippingResponse {
    private Long id;
    private Long orderId;
    private Long carrierId;
    private Integer freightMode;
    private String vehiclePlate;
    private String vehicleState;
    private String rntrcCode;
    private Integer packageQuantity;
    private String packageSpecies;
    private String packageBrand;
    private String packageNumbering;
    private BigDecimal grossWeightKg;
    private BigDecimal netWeightKg;
    private String trackingCode;
    private String shippingNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
