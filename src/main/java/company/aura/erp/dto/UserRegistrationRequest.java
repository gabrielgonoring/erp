package company.aura.erp.dto;

public record UserRegistrationRequest(
        Long companyId,
        String name,
        String email,
        String password
) {}
