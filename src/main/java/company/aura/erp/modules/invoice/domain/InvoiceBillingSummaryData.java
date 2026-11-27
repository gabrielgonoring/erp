package company.aura.erp.modules.invoice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceBillingSummaryData {
    private String invoiceNumber;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
}
