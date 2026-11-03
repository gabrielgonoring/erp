package company.aura.erp.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemUpdateRequest(
        @DecimalMin(value = "0.0001", message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,

        @DecimalMin(value = "0.00", message = "O preço unitário não pode ser negativo.")
        BigDecimal unitPrice,

        @DecimalMin(value = "0.00", message = "O desconto não pode ser negativo.")
        BigDecimal discountAmount,

        Long professionalId,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description
) {}
