package company.aura.erp.mapper;

import company.aura.erp.dto.serviceorder.ServiceOrderUpdateCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderUpdateResponse;
import company.aura.erp.entity.ServiceOrderUpdateEntity;

public class ServiceOrderUpdateMapper {

    public static ServiceOrderUpdateEntity toEntity(
            ServiceOrderUpdateCreateRequest request,
            Long serviceOrderId,
            Long companyId
    ) {
        return ServiceOrderUpdateEntity.builder()
                .companyId(companyId)
                .serviceOrderId(serviceOrderId)
                .eventType(request.getEventType())
                .description(request.getDescription().trim())
                .build();
    }

    public static ServiceOrderUpdateResponse fromEntity(ServiceOrderUpdateEntity entity) {
        if (entity == null) {
            return null;
        }
        return ServiceOrderUpdateResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .serviceOrderId(entity.getServiceOrderId())
                .eventType(entity.getEventType())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
