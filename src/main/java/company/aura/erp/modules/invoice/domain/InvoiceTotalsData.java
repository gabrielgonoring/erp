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
public class InvoiceTotalsData {
    @Builder.Default
    private BigDecimal icmsBaseAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal icmsAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal icmsExemptAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal fcpAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal icmsStBaseAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal icmsStAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal productsAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal freightAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal insuranceAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal importTaxAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ipiAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ipiReturnedAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal pisAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cofinsAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal additionalExpenses = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalTaxesAmount = BigDecimal.ZERO;
}
