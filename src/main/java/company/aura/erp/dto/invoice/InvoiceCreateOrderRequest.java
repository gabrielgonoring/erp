package company.aura.erp.dto.invoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreateOrderRequest {

    @NotNull(message = "O ID da nota fiscal é obrigatório.")
    private Long invoiceId;

    @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
    private String notes;
}
