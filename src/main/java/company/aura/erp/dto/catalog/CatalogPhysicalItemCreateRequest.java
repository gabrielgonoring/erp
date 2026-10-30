package company.aura.erp.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CatalogPhysicalItemCreateRequest(
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

        @NotBlank(message = "NCM é obrigatório.")
        @Pattern(regexp = "\\d{8}", message = "NCM deve conter exatamente 8 dígitos numéricos.")
        String ncm,

        @NotBlank(message = "CFOP padrão é obrigatório.")
        @Pattern(regexp = "\\d{4}", message = "CFOP padrão deve conter exatamente 4 dígitos numéricos.")
        String defaultCfop,

        @Size(max = 4, message = "CSOSN padrão deve ter no máximo 4 caracteres.")
        String defaultCsosn,

        @Size(max = 14, message = "GTIN/EAN deve ter no máximo 14 caracteres.")
        String gtinEan,

        @Size(max = 7, message = "CEST deve ter no máximo 7 caracteres.")
        @Pattern(regexp = "^$|\\d{7}", message = "CEST deve conter exatamente 7 dígitos numéricos quando informado.")
        String cest,

        @Min(value = 0, message = "Origem deve ser entre 0 e 8.")
        @Max(value = 8, message = "Origem deve ser entre 0 e 8.")
        Integer origin,

        @DecimalMin(value = "0.00", message = "Alíquota de crédito de ICMS não pode ser negativa.")
        @DecimalMax(value = "100.00", message = "Alíquota de crédito de ICMS não pode ultrapassar 100%.")
        @Digits(integer = 3, fraction = 2, message = "Alíquota de crédito de ICMS deve ter no máximo 3 inteiros e 2 decimais.")
        BigDecimal defaultIcmsCreditRate,

        @DecimalMin(value = "0.00", message = "Alíquota de ICMS não pode ser negativa.")
        @DecimalMax(value = "100.00", message = "Alíquota de ICMS não pode ultrapassar 100%.")
        @Digits(integer = 3, fraction = 2, message = "Alíquota de ICMS deve ter no máximo 3 inteiros e 2 decimais.")
        BigDecimal icmsRate,

        @DecimalMin(value = "0.00", message = "Alíquota de PIS não pode ser negativa.")
        @DecimalMax(value = "100.00", message = "Alíquota de PIS não pode ultrapassar 100%.")
        @Digits(integer = 3, fraction = 2, message = "Alíquota de PIS deve ter no máximo 3 inteiros e 2 decimais.")
        BigDecimal pisRate,

        @DecimalMin(value = "0.00", message = "Alíquota de COFINS não pode ser negativa.")
        @DecimalMax(value = "100.00", message = "Alíquota de COFINS não pode ultrapassar 100%.")
        @Digits(integer = 3, fraction = 2, message = "Alíquota de COFINS deve ter no máximo 3 inteiros e 2 decimais.")
        BigDecimal cofinsRate
) {}
