package company.aura.erp.dto.order;

import company.aura.erp.enums.CatalogItemType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OrderItemCreateRequest(
        Long catalogItemId,

        @NotNull(message = "O tipo de item (PHYSICAL ou SERVICE) é obrigatório.")
        CatalogItemType itemType,

        @NotBlank(message = "A descrição do item é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "A quantidade é obrigatória.")
        @DecimalMin(value = "0.0001", message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,

        @NotNull(message = "O preço unitário é obrigatório.")
        @DecimalMin(value = "0.00", message = "O preço unitário não pode ser negativo.")
        BigDecimal unitPrice,

        @DecimalMin(value = "0.00", message = "O valor de desconto não pode ser negativo.")
        @DecimalMax(value = "100.00", message = "O valor de desconto não pode ser maior que 100.")
        BigDecimal discountPercentage,

        Long professionalId
) {}
