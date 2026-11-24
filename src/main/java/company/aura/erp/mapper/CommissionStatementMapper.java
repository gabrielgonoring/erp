package company.aura.erp.mapper;

import company.aura.erp.dto.commission.CommissionEntryResponse;
import company.aura.erp.dto.commission.CommissionStatementResponse;
import company.aura.erp.dto.commission.CommissionStatementSummaryResponse;
import company.aura.erp.entity.CommissionEntryEntity;
import company.aura.erp.entity.CommissionStatementEntity;

import java.util.Collections;
import java.util.List;

public class CommissionStatementMapper {

    public static CommissionStatementResponse fromEntity(
            CommissionStatementEntity entity,
            List<CommissionEntryEntity> entries
    ) {
        if (entity == null) {
            return null;
        }
        List<CommissionEntryResponse> entryResponses = entries != null
                ? entries.stream().map(CommissionEntryMapper::fromEntity).toList()
                : Collections.emptyList();

        return CommissionStatementResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .collaboratorId(entity.getCollaboratorId())
                .collaboratorName(entity.getCollaboratorName())
                .payableId(entity.getPayableId())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .dueDate(entity.getDueDate())
                .totalSalesAmount(entity.getTotalSalesAmount())
                .totalCommissionAmount(entity.getTotalCommissionAmount())
                .status(entity.getStatus())
                .paidAt(entity.getPaidAt())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .entries(entryResponses)
                .build();
    }

    public static CommissionStatementSummaryResponse toSummaryResponse(CommissionStatementEntity entity) {
        if (entity == null) {
            return null;
        }
        return CommissionStatementSummaryResponse.builder()
                .id(entity.getId())
                .collaboratorId(entity.getCollaboratorId())
                .collaboratorName(entity.getCollaboratorName())
                .payableId(entity.getPayableId())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .dueDate(entity.getDueDate())
                .totalSalesAmount(entity.getTotalSalesAmount())
                .totalCommissionAmount(entity.getTotalCommissionAmount())
                .status(entity.getStatus())
                .paidAt(entity.getPaidAt())
                .build();
    }
}
