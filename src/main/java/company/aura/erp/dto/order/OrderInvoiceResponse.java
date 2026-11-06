package company.aura.erp.dto.order;

import company.aura.erp.enums.InvoicePurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInvoiceResponse {
    private Long id;
    private Long companyId;
    private Long orderId;
    private Long invoiceId;
    private InvoicePurpose invoicePurpose;
    private String notes;
    private LocalDateTime createdAt;
}
