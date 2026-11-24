package company.aura.erp.mapper;

import company.aura.erp.dto.commission.CommissionRuleCreateRequest;
import company.aura.erp.dto.commission.CommissionRuleResponse;
import company.aura.erp.entity.CommissionRuleEntity;
import company.aura.erp.enums.CommissionRuleStatus;

public class CommissionRuleMapper {

    public static CommissionRuleEntity toEntity(CommissionRuleCreateRequest request, Long companyId) {
        return CommissionRuleEntity.builder()
                .companyId(companyId)
                .collaboratorId(request.getCollaboratorId())
                .categoryId(request.getCategoryId())
                .catalogItemId(request.getCatalogItemId())
                .commissionPercentage(request.getCommissionPercentage())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .status(CommissionRuleStatus.ACTIVE)
                .build();
    }

    public static CommissionRuleResponse fromEntity(
            CommissionRuleEntity entity,
            String collaboratorName,
            String categoryName,
            String catalogItemDescription
    ) {
        if (entity == null) {
            return null;
        }
        return CommissionRuleResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .collaboratorId(entity.getCollaboratorId())
                .collaboratorName(collaboratorName)
                .categoryId(entity.getCategoryId())
                .categoryName(categoryName)
                .catalogItemId(entity.getCatalogItemId())
                .catalogItemDescription(catalogItemDescription)
                .commissionPercentage(entity.getCommissionPercentage())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
