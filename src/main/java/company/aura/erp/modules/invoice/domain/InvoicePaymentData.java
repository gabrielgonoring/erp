package company.aura.erp.modules.invoice.domain;

import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.modules.invoice.enumerate.SefazPaymentCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentData {
    private Integer paymentIndicator;
    private String sefazCode;
    private SefazPaymentCode sefazPaymentCode;
    private PaymentMethod paymentMethod;
    private String description;
    private BigDecimal amount;
    private Integer cardIntegrationType;
    private String cardAcquirerCnpj;
    private String cardBrandCode;
    private String authorizationCode;
}
