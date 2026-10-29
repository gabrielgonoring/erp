package company.aura.erp.dto.company;

import company.aura.erp.entity.CompanyEntity;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String corporateName,
        String tradeName,
        String cnpj,
        String status,
        String stateRegistration,
        String municipalRegistration,
        Integer taxRegime,
        String street,
        String number,
        String complement,
        String neighborhood,
        String ibgeCityCode,
        String city,
        String state,
        String zipCode,
        String countryCode,
        String countryName,
        LocalDateTime certificateExpirationDate,
        String notaasCompanyId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanyResponse fromEntity(CompanyEntity entity) {
        return new CompanyResponse(
                entity.getId(),
                entity.getCorporateName(),
                entity.getTradeName(),
                entity.getCnpj(),
                entity.getStatus(),
                entity.getStateRegistration(),
                entity.getMunicipalRegistration(),
                entity.getTaxRegime(),
                entity.getStreet(),
                entity.getNumber(),
                entity.getComplement(),
                entity.getNeighborhood(),
                entity.getIbgeCityCode(),
                entity.getCity(),
                entity.getState(),
                entity.getZipCode(),
                entity.getCountryCode(),
                entity.getCountryName(),
                entity.getCertificateExpirationDate(),
                entity.getNotaasCompanyId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
