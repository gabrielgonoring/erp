package company.aura.erp.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemUpdateRequest {
    private Long catalogItemId;

    @DecimalMin(value = "0.0001", message = "A quantidade deve ser maior que zero.")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "O desconto não pode ser negativo.")
    private BigDecimal discountAmount;

    private Long professionalId;

    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
    private String description;
}
