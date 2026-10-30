package company.aura.erp.dto.catalog;

import company.aura.erp.entity.CatalogPhysicalItemEntity;

import java.math.BigDecimal;

public record CatalogPhysicalItemResponse(
        Long catalogItemId,
        String ncm,
        String defaultCfop,
        String defaultCsosn,
        String gtinEan,
        String cest,
        Integer origin,
        BigDecimal defaultIcmsCreditRate,
        BigDecimal icmsRate,
        BigDecimal pisRate,
        BigDecimal cofinsRate
) {
    public static CatalogPhysicalItemResponse fromEntity(CatalogPhysicalItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CatalogPhysicalItemResponse(
                entity.getCatalogItemId(),
                entity.getNcm(),
                entity.getDefaultCfop(),
                entity.getDefaultCsosn(),
                entity.getGtinEan(),
                entity.getCest(),
                entity.getOrigin(),
                entity.getDefaultIcmsCreditRate(),
                entity.getIcmsRate(),
                entity.getPisRate(),
                entity.getCofinsRate()
        );
    }
}
