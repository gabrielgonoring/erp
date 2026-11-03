package company.aura.erp.dto.order;

import company.aura.erp.enums.CatalogItemType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OrderItemCreateRequest(
        Long catalogItemId,

        @NotBlank(message = "A descrição do item é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "A quantidade é obrigatória.")
        @DecimalMin(value = "0.0001", message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,

        @DecimalMin(value = "0.00", message = "O valor de desconto não pode ser negativo.")
        BigDecimal discountAmount,

        Long professionalId
) {}
