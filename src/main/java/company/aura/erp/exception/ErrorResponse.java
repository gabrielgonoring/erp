package company.aura.erp.exception;

public record ErrorResponse(
        String error,
        String description
) {}
