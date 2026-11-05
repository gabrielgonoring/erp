package company.aura.erp.dto.order;

public record OrderUpdateRequest(
        Long partnerId,
        String notes
) {}
