package company.aura.erp.dto.order;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "O parceiro (cliente) é obrigatório.")
        Long partnerId,

        String notes,

        List<@Valid OrderItemCreateRequest> items,

        @Valid
        OrderShippingRequest shipping
) {
    public OrderCreateRequest(Long partnerId, String notes, List<@Valid OrderItemCreateRequest> items) {
        this(partnerId, notes, items, null);
    }
}
