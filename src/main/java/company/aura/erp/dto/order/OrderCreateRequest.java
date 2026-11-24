package company.aura.erp.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    @NotNull(message = "O parceiro (cliente) é obrigatório.")
    private Long partnerId;

    private String notes;

    private List<@Valid OrderItemCreateRequest> items;

    @Valid
    private OrderShippingRequest shipping;

    public OrderCreateRequest(Long partnerId, String notes, List<@Valid OrderItemCreateRequest> items) {
        this(partnerId, notes, items, null);
    }
}
