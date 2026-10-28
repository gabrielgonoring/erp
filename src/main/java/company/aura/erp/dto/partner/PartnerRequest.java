package company.aura.erp.dto.partner;

import company.aura.erp.enums.PersonType;

public record PartnerRequest(
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
        String countryName
) {}
