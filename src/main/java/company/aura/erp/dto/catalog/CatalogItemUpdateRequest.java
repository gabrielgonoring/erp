package company.aura.erp.dto.catalog;

import company.aura.erp.enums.CatalogItemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CatalogItemUpdateRequest(
        CatalogItemType type,

        @Size(max = 50, message = "Código interno deve ter no máximo 50 caracteres.")
        String internalCode,

        @NotBlank(message = "Descrição do item é obrigatória.")
        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "Valor unitário é obrigatório.")
        @DecimalMin(value = "0.01", message = "Valor unitário deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "Valor unitário deve ter no máximo 10 dígitos inteiros e 2 decimais.")
        BigDecimal unitPrice,

        @NotBlank(message = "Unidade de medida é obrigatória.")
        @Size(max = 10, message = "Unidade de medida deve ter no máximo 10 caracteres.")
        String unitOfMeasure,

        @Valid
        CatalogPhysicalItemRequest physicalItem,

        @Valid
        CatalogServiceItemRequest serviceItem
) {}
