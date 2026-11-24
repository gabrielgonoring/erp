package company.aura.erp.dto.serviceorder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderShippingRequest {

    private Integer freightMode;

    private Long carrierId;

    @Size(max = 8, message = "A placa do veículo deve ter no máximo 8 caracteres.")
    private String vehiclePlate;

    @Size(max = 2, message = "A UF do veículo deve ter no máximo 2 caracteres.")
    private String vehicleState;

    @Size(max = 20, message = "O código RNTRC deve ter no máximo 20 caracteres.")
    private String rntrcCode;

    @Min(value = 1, message = "A quantidade de volumes deve ser no mínimo 1.")
    private Integer packageQuantity;

    @Size(max = 50, message = "A espécie dos volumes deve ter no máximo 50 caracteres.")
    private String packageSpecies;

    @Size(max = 50, message = "A marca dos volumes deve ter no máximo 50 caracteres.")
    private String packageBrand;

    @Size(max = 50, message = "A numeração dos volumes deve ter no máximo 50 caracteres.")
    private String packageNumbering;

    @DecimalMin(value = "0.000", message = "O peso bruto não pode ser negativo.")
    private BigDecimal grossWeightKg;

    @DecimalMin(value = "0.000", message = "O peso líquido não pode ser negativo.")
    private BigDecimal netWeightKg;

    @Size(max = 100, message = "O código de rastreio deve ter no máximo 100 caracteres.")
    private String trackingCode;

    @Size(max = 255, message = "As observações de frete devem ter no máximo 255 caracteres.")
    private String shippingNotes;

    @DecimalMin(value = "0.00", message = "O valor do frete não pode ser negativo.")
    private BigDecimal freightAmount;
}
