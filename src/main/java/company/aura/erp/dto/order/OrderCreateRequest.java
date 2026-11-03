package company.aura.erp.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "O parceiro (cliente) é obrigatório.")
        Long partnerId,

        @DecimalMin(value = "0.00", message = "O valor do frete não pode ser negativo.")
        BigDecimal freightAmount,

        String notes,

        List<@Valid OrderItemCreateRequest> items
) {}
