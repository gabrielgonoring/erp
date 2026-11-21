package company.aura.erp.dto.order;

import jakarta.validation.constraints.NotNull;

public record OrderUpdateRequest(
        @NotNull(message = "Necessário informar o parceiro")
        Long partnerId,
        String notes
) {}
