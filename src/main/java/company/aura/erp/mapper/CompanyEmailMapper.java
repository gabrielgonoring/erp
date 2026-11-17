package company.aura.erp.mapper;

import company.aura.erp.dto.email.CompanyEmailResponse;
import company.aura.erp.entity.CompanyEmailConfigEntity;

public class CompanyEmailMapper {

    public static CompanyEmailResponse fromEntity(CompanyEmailConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        return CompanyEmailResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .notificationType(entity.getNotificationType())
                .fromEmail(entity.getFromEmail())
                .fromName(entity.getFromName())
                .replyTo(entity.getReplyTo())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
