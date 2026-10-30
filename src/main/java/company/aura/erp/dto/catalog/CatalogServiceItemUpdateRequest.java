package company.aura.erp.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CatalogServiceItemUpdateRequest(
        @Size(max = 50, message = "Código interno deve ter no máximo 50 caracteres.")
        String internalCode,

        @NotBlank(message = "Descrição/Nome do item é obrigatória.")
        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "Valor unitário é obrigatório.")
        @DecimalMin(value = "0.01", message = "Valor unitário deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "Valor unitário deve ter no máximo 10 dígitos inteiros e 2 decimais.")
        BigDecimal unitPrice,

        @NotBlank(message = "Unidade de medida é obrigatória.")
        @Size(max = 10, message = "Unidade de medida deve ter no máximo 10 caracteres.")
        String unitOfMeasure,

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
