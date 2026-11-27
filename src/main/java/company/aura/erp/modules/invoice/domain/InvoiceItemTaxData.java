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
public class InvoiceItemTaxData {
    private BigDecimal totalTaxesAmount;
    private Integer origin;
    private String icmsCstOrCsosn;
    private BigDecimal icmsBaseAmount;
    private BigDecimal icmsRate;
    private BigDecimal icmsAmount;
    private BigDecimal icmsCreditRate;
    private BigDecimal icmsCreditAmount;
    private BigDecimal icmsStBaseAmount;
    private BigDecimal icmsStAmount;
    private String ipiCst;
    private BigDecimal ipiBaseAmount;
    private BigDecimal ipiRate;
    private BigDecimal ipiAmount;
    private String pisCst;
    private BigDecimal pisBaseAmount;
    private BigDecimal pisRate;
    private BigDecimal pisAmount;
    private String cofinsCst;
    private BigDecimal cofinsBaseAmount;
    private BigDecimal cofinsRate;
    private BigDecimal cofinsAmount;
}
