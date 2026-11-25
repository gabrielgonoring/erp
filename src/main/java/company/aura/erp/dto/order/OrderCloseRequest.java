package company.aura.erp.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCloseRequest {
    private List<@Valid ReceivableRequest> receivables;

    @Valid
    private OrderShippingRequest shipping;

    @DecimalMin(value = "0.00", message = "O valor de desconto não pode ser negativo.")
    private BigDecimal discountAmount;
}
