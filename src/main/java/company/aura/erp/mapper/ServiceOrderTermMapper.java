package company.aura.erp.mapper;

import company.aura.erp.dto.serviceorder.ServiceOrderTermCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderTermResponse;
import company.aura.erp.dto.serviceorder.ServiceOrderTermSummaryResponse;
import company.aura.erp.entity.ServiceOrderTermEntity;
import company.aura.erp.enums.ServiceOrderTermStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ServiceOrderTermMapper {

    public static ServiceOrderTermEntity toEntity(ServiceOrderTermCreateRequest request, Long companyId) {
        return ServiceOrderTermEntity.builder()
                .companyId(companyId)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .status(ServiceOrderTermStatus.ACTIVE)
                .build();
    }

    public static ServiceOrderTermResponse fromEntity(ServiceOrderTermEntity entity) {
        if (entity == null) {
            return null;
        }
        return ServiceOrderTermResponse.builder()
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

    public static ServiceOrderTermSummaryResponse toSummaryResponse(ServiceOrderTermEntity entity) {
        if (entity == null) {
            return null;
        }
        return ServiceOrderTermSummaryResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .build();
    }
}
