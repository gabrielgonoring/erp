package company.aura.erp.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderShippingRequest(

        Integer freightMode,

        Long carrierId,

        @Size(max = 8, message = "A placa do veículo deve ter no máximo 8 caracteres.")
        String vehiclePlate,

        @Size(max = 2, message = "A UF do veículo deve ter no máximo 2 caracteres.")
        String vehicleState,

        @Size(max = 20, message = "O código RNTRC deve ter no máximo 20 caracteres.")
        String rntrcCode,

        @Min(value = 1, message = "A quantidade de volumes deve ser no mínimo 1.")
        Integer packageQuantity,

        @Size(max = 50, message = "A espécie dos volumes deve ter no máximo 50 caracteres.")
        String packageSpecies,

        @Size(max = 50, message = "A marca dos volumes deve ter no máximo 50 caracteres.")
        String packageBrand,

        @Size(max = 50, message = "A numeração dos volumes deve ter no máximo 50 caracteres.")
        String packageNumbering,

        @DecimalMin(value = "0.000", message = "O peso bruto não pode ser negativo.")
        BigDecimal grossWeightKg,

        @DecimalMin(value = "0.000", message = "O peso líquido não pode ser negativo.")
        BigDecimal netWeightKg,

        @Size(max = 100, message = "O código de rastreio deve ter no máximo 100 caracteres.")
        String trackingCode,

        @Size(max = 255, message = "As observações de frete devem ter no máximo 255 caracteres.")
        String shippingNotes,

        @DecimalMin(value = "0.00", message = "O valor do frete não pode ser negativo.")
        BigDecimal freightAmount
) {}
