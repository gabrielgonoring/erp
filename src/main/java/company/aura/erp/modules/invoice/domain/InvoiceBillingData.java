package company.aura.erp.modules.invoice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceBillingData {
    private InvoiceBillingSummaryData invoiceSummary;

    @Builder.Default
    private List<InvoiceInstallmentData> installments = new ArrayList<>();

    @Builder.Default
    private List<InvoicePaymentData> payments = new ArrayList<>();

    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;
}
