package company.aura.erp.dto.invoice;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceImportCatalogItemsRequest {

    @NotNull(message = "O ID da nota fiscal é obrigatório.")
    private Long invoiceId;

    private Long categoryId;
}
