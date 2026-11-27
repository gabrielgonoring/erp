package company.aura.erp.dto.invoice;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class InvoiceCreateRequest {
    @NotNull(message = "O ID do parceiro (cliente/destinatário) é obrigatório.")
    private Long partnerId;
}
