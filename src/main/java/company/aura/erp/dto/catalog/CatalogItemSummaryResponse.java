package company.aura.erp.dto.catalog;

import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.enums.CatalogItemType;

import java.math.BigDecimal;

public record CatalogItemSummaryResponse(
        Long id,
        Long categoryId,
        String internalCode,
        String name,
        CatalogItemType type,
        String description,
        BigDecimal unitPrice
) {
    public CatalogItemSummaryResponse(Long id, String internalCode, String name, String description, BigDecimal unitPrice) {
        this(id, null, internalCode, name, null, description, unitPrice);
    }

    public CatalogItemSummaryResponse(Long id, String internalCode, String name, CatalogItemType type, String description, BigDecimal unitPrice) {
        this(id, null, internalCode, name, type, description, unitPrice);
    }

    public static CatalogItemSummaryResponse fromEntity(CatalogItemEntity entity) {
        return new CatalogItemSummaryResponse(
                entity.getId(),
                entity.getCategoryId(),
                entity.getInternalCode(),
                entity.getDescription(),
                entity.getType(),
                entity.getDescription(),
                entity.getUnitPrice()
        );
    }
}
