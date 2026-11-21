package company.aura.erp.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCloseRequest {
    @NotEmpty(message = "Informe ao menos uma parcela de pagamento.")
    private List<@Valid ReceivableRequest> receivables;

    @Valid
    private OrderShippingRequest shipping;

    public OrderCloseRequest(List<@Valid ReceivableRequest> receivables) {
        this(receivables, null);
    }
}
