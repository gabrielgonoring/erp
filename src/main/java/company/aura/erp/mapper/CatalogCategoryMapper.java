package company.aura.erp.mapper;

import company.aura.erp.dto.catalog.CatalogCategoryCreateRequest;
import company.aura.erp.dto.catalog.CatalogCategoryResponse;
import company.aura.erp.dto.catalog.CatalogCategorySummaryResponse;
import company.aura.erp.entity.CatalogCategoryEntity;
import company.aura.erp.enums.CatalogCategoryStatus;

public class CatalogCategoryMapper {

    public static CatalogCategoryEntity toEntity(CatalogCategoryCreateRequest request, Long companyId) {
        return CatalogCategoryEntity.builder()
                .companyId(companyId)
                .name(request.getName().trim())
                .status(CatalogCategoryStatus.ACTIVE)
                .build();
    }

    public static CatalogCategoryResponse fromEntity(CatalogCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return CatalogCategoryResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .name(entity.getName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static CatalogCategorySummaryResponse toSummaryResponse(CatalogCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return CatalogCategorySummaryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .build();
    }
}
