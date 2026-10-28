package company.aura.erp.dto.partner;

import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.enums.PersonType;

import java.time.LocalDateTime;

public record PartnerResponse(
        Long id,
        Long companyId,
        Boolean isCustomer,
        Boolean isSupplier,
        PersonType personType,
        String document,
        String name,
        Integer indicatorIe,
        String stateRegistration,
        String email,
        String phone,
        String status,
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PartnerResponse fromEntity(PartnerEntity entity) {
        return new PartnerResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getIsCustomer(),
                entity.getIsSupplier(),
                entity.getPersonType(),
                entity.getDocument(),
                entity.getName(),
                entity.getIndicatorIe(),
                entity.getStateRegistration(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getStatus(),
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
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
