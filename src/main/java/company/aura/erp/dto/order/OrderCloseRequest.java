package company.aura.erp.dto.order;

import jakarta.validation.Valid;
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
    private List<@Valid ReceivableRequest> receivables;

    @Valid
    private OrderShippingRequest shipping;

    public OrderCloseRequest(List<@Valid ReceivableRequest> receivables) {
        this(receivables, null);
    }
}
