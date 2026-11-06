package company.aura.erp.dto.order;

import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.InvoicePurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderExternalInvoiceRequest {
    private DocumentType documentType;
    private String externalReference;
    private Integer operationType;
    private BigDecimal totalAmount;
    private InvoicePurpose invoicePurpose;
    private String notes;
}
