package company.aura.erp.mapper;

import company.aura.erp.dto.collaborator.CollaboratorCreateRequest;
import company.aura.erp.dto.collaborator.CollaboratorResponse;
import company.aura.erp.dto.collaborator.CollaboratorSummaryResponse;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.enums.CollaboratorStatus;

public class CollaboratorMapper {

    public static CollaboratorEntity toEntity(CollaboratorCreateRequest request, Long companyId) {
        if (request == null) {
            return null;
        }
        return CollaboratorEntity.builder()
                .companyId(companyId)
                .userId(request.getUserId())
                .name(request.getName())
                .cpf(request.getCpf())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(CollaboratorStatus.ACTIVE.name())
                .street(request.getStreet())
                .number(request.getNumber())
                .complement(request.getComplement())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .ibgeCityCode(request.getIbgeCityCode())
                .rg(request.getRg())
                .birthDate(request.getBirthDate())
                .admissionDate(request.getAdmissionDate())
                .resignationDate(request.getResignationDate())
                .jobTitle(request.getJobTitle())
                .pisPasep(request.getPisPasep())
                .build();
    }

    public static CollaboratorResponse toResponse(CollaboratorEntity entity) {
        if (entity == null) {
            return null;
        }
        return CollaboratorResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .cpf(entity.getCpf())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .status(entity.getStatus())
                .street(entity.getStreet())
                .number(entity.getNumber())
                .complement(entity.getComplement())
                .neighborhood(entity.getNeighborhood())
                .city(entity.getCity())
                .state(entity.getState())
                .zipCode(entity.getZipCode())
                .ibgeCityCode(entity.getIbgeCityCode())
                .rg(entity.getRg())
                .birthDate(entity.getBirthDate())
                .admissionDate(entity.getAdmissionDate())
                .resignationDate(entity.getResignationDate())
                .jobTitle(entity.getJobTitle())
                .pisPasep(entity.getPisPasep())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static CollaboratorSummaryResponse toSummaryResponse(CollaboratorEntity entity) {
        if (entity == null) {
            return null;
        }
        return CollaboratorSummaryResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .nome(entity.getName())
                .cpf(entity.getCpf())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .build();
    }

    public static CollaboratorResponse fromEntity(CollaboratorEntity entity) {
        return toResponse(entity);
    }

    public static CollaboratorSummaryResponse fromEntitySummary(CollaboratorEntity entity) {
        return toSummaryResponse(entity);
    }
}
