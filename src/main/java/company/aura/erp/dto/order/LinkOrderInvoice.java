package company.aura.erp.dto.order;

import company.aura.erp.enums.InvoicePurpose;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkOrderInvoice {
    @NotNull(message = "É obrigatório informar o propósito")
    private InvoicePurpose purpose;
    private String notes;
}
