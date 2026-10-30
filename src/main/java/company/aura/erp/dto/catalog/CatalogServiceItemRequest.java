package company.aura.erp.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CatalogServiceItemRequest(
        @NotBlank(message = "Código de serviço LC 116 é obrigatório.")
        @Size(max = 10, message = "Código de serviço LC 116 deve ter no máximo 10 caracteres.")
        String lc116ServiceCode,

        @Size(max = 20, message = "Código de tributação municipal deve ter no máximo 20 caracteres.")
        String cityTaxCode,

        @NotNull(message = "Alíquota de ISS é obrigatória.")
        @DecimalMin(value = "0.00", message = "Alíquota de ISS não pode ser negativa.")
        @DecimalMax(value = "100.00", message = "Alíquota de ISS não pode ultrapassar 100%.")
        @Digits(integer = 3, fraction = 2, message = "Alíquota de ISS deve ter no máximo 3 dígitos inteiros e 2 decimais.")
        BigDecimal issRate,

        Boolean issWithheld
) {}
