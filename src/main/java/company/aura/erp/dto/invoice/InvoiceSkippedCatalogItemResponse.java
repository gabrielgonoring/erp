package company.aura.erp.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSkippedCatalogItemResponse {
    private Integer itemNumber;
    private String productCode;
    private String gtinEan;
    private String description;
    private String reason;
}
