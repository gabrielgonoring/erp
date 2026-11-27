package company.aura.erp.dto.catalog;

import java.math.BigDecimal;

public record CatalogItemSearchResponse(
        Long id,
        String internalCode,
        BigDecimal unitPrice,
        String description,
        String unitOfMeasure
) {
}
