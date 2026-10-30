package company.aura.erp.dto.catalog;

import company.aura.erp.entity.CatalogItemEntity;

import java.math.BigDecimal;

public record CatalogItemSummaryResponse(
        Long id,
        String internalCode,
        String name,
        String description,
        BigDecimal unitPrice
) {
    public static CatalogItemSummaryResponse fromEntity(CatalogItemEntity entity) {
        return new CatalogItemSummaryResponse(
                entity.getId(),
                entity.getInternalCode(),
                entity.getDescription(),
                entity.getDescription(),
                entity.getUnitPrice()
        );
    }
}
