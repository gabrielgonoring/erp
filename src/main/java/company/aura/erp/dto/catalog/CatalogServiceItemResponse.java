package company.aura.erp.dto.catalog;

import company.aura.erp.entity.CatalogServiceItemEntity;

import java.math.BigDecimal;

public record CatalogServiceItemResponse(
        Long catalogItemId,
        String lc116ServiceCode,
        String cityTaxCode,
        BigDecimal issRate,
        Boolean issWithheld
) {
    public static CatalogServiceItemResponse fromEntity(CatalogServiceItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CatalogServiceItemResponse(
                entity.getCatalogItemId(),
                entity.getLc116ServiceCode(),
                entity.getCityTaxCode(),
                entity.getIssRate(),
                entity.getIssWithheld()
        );
    }
}
