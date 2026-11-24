package company.aura.erp.dto.catalog;

import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.enums.CatalogItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CatalogItemResponse(
        Long id,
        Long companyId,
        Long categoryId,
        CatalogItemType type,
        String internalCode,
        String status,
        String description,
        BigDecimal unitPrice,
        String unitOfMeasure,
        CatalogPhysicalItemResponse physicalItem,
        CatalogServiceItemResponse serviceItem,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public CatalogItemResponse(
            Long id,
            Long companyId,
            CatalogItemType type,
            String internalCode,
            String status,
            String description,
            BigDecimal unitPrice,
            String unitOfMeasure,
            CatalogPhysicalItemResponse physicalItem,
            CatalogServiceItemResponse serviceItem,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, companyId, null, type, internalCode, status, description, unitPrice, unitOfMeasure,
                physicalItem, serviceItem, createdAt, updatedAt);
    }

    public static CatalogItemResponse fromEntity(CatalogItemEntity entity) {
        return new CatalogItemResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getCategoryId(),
                entity.getType(),
                entity.getInternalCode(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getUnitPrice(),
                entity.getUnitOfMeasure(),
                CatalogPhysicalItemResponse.fromEntity(entity.getPhysicalItem()),
                CatalogServiceItemResponse.fromEntity(entity.getServiceItem()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
