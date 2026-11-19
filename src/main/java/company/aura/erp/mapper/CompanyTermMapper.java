package company.aura.erp.mapper;

import company.aura.erp.dto.company.CompanyTermCreateRequest;
import company.aura.erp.dto.company.CompanyTermResponse;
import company.aura.erp.dto.company.CompanyTermSummaryResponse;
import company.aura.erp.entity.CompanyTermEntity;
import company.aura.erp.enums.CompanyTermStatus;

public class CompanyTermMapper {

    public static CompanyTermEntity toEntity(CompanyTermCreateRequest request, Long companyId) {
        return CompanyTermEntity.builder()
                .companyId(companyId)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .status(CompanyTermStatus.ACTIVE)
                .build();
    }

    public static CompanyTermResponse fromEntity(CompanyTermEntity entity) {
        if (entity == null) {
            return null;
        }
        return CompanyTermResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .isDefault(entity.getIsDefault())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static CompanyTermSummaryResponse toSummaryResponse(CompanyTermEntity entity) {
        if (entity == null) {
            return null;
        }
        return CompanyTermSummaryResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .isDefault(entity.getIsDefault())
                .build();
    }
}
