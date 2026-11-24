package company.aura.erp.mapper;

import company.aura.erp.dto.commission.CommissionEntryResponse;
import company.aura.erp.entity.CommissionEntryEntity;

public class CommissionEntryMapper {

    public static CommissionEntryResponse fromEntity(CommissionEntryEntity entity) {
        if (entity == null) {
            return null;
        }
        return CommissionEntryResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .collaboratorId(entity.getCollaboratorId())
                .collaboratorName(entity.getCollaboratorName())
                .statementId(entity.getStatementId())
                .sourceType(entity.getSourceType())
                .sourceDocumentId(entity.getSourceDocumentId())
                .sourceItemId(entity.getSourceItemId())
                .catalogItemId(entity.getCatalogItemId())
                .categoryId(entity.getCategoryId())
                .itemDescription(entity.getItemDescription())
                .baseAmount(entity.getBaseAmount())
                .appliedPercentage(entity.getAppliedPercentage())
                .commissionAmount(entity.getCommissionAmount())
                .ruleId(entity.getRuleId())
                .ruleSource(entity.getRuleSource())
                .status(entity.getStatus())
                .occurredAt(entity.getOccurredAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
