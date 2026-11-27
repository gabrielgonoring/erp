package company.aura.erp.modules.invoice.parser.extractor;

import company.aura.erp.modules.invoice.domain.InvoiceBillingData;
import company.aura.erp.modules.invoice.domain.InvoiceBillingSummaryData;
import company.aura.erp.modules.invoice.domain.InvoiceInstallmentData;
import company.aura.erp.modules.invoice.domain.InvoicePaymentData;
import company.aura.erp.modules.invoice.enumerate.SefazPaymentCode;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.InvoiceXmlSectionExtractor;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class BillingXmlExtractor implements InvoiceXmlSectionExtractor<InvoiceBillingData> {

    @Override
    public InvoiceBillingData extract(InvoiceXmlContext context) {
        log.info("extract() - START - Extracting billing and payment data (<cobr>, <pag>)");
        XmlElementReader reader = context.getReader();
        Element infNFe = context.getInfNFeElement();

        Element cobr = reader.findChild(infNFe, "cobr").orElse(null);
        Element pag = reader.findChild(infNFe, "pag").orElse(null);

        InvoiceBillingSummaryData summary = extractBillingSummary(reader, cobr);
        List<InvoiceInstallmentData> installments = extractInstallments(reader, cobr);
        List<InvoicePaymentData> payments = extractPayments(reader, pag);
        BigDecimal changeAmount = pag != null ? reader.getBigDecimalOrZero(pag, "vTroco") : BigDecimal.ZERO;

        InvoiceBillingData billing = InvoiceBillingData.builder()
                .invoiceSummary(summary)
                .installments(installments)
                .payments(payments)
                .changeAmount(changeAmount)
                .build();

        log.info("extract() - END - hasInvoiceSummary={}, totalInstallments={}, totalPayments={}",
                summary != null, installments.size(), payments.size());
        return billing;
    }

    private InvoiceBillingSummaryData extractBillingSummary(XmlElementReader reader, Element cobr) {
        if (cobr == null) {
            return null;
        }
        Element fat = reader.findChild(cobr, "fat").orElse(null);
        if (fat == null) {
            return null;
        }
        return InvoiceBillingSummaryData.builder()
                .invoiceNumber(reader.getText(fat, "nFat"))
                .originalAmount(reader.getBigDecimalOrZero(fat, "vOrig"))
                .discountAmount(reader.getBigDecimalOrZero(fat, "vDesc"))
                .netAmount(reader.getBigDecimalOrZero(fat, "vLiq"))
                .build();
    }

    private List<InvoiceInstallmentData> extractInstallments(XmlElementReader reader, Element cobr) {
        if (cobr == null) {
            return Collections.emptyList();
        }
        return reader.findChildren(cobr, "dup").stream()
                .map(dup -> InvoiceInstallmentData.builder()
                        .installmentNumber(reader.getText(dup, "nDup"))
                        .dueDate(reader.getLocalDate(dup, "dVenc"))
                        .amount(reader.getBigDecimalOrZero(dup, "vDup"))
                        .build())
                .toList();
    }

    private List<InvoicePaymentData> extractPayments(XmlElementReader reader, Element pag) {
        if (pag == null) {
            return Collections.emptyList();
        }
        return reader.findChildren(pag, "detPag").stream()
                .map(detPag -> extractSinglePayment(reader, detPag))
                .toList();
    }

    private InvoicePaymentData extractSinglePayment(XmlElementReader reader, Element detPag) {
        String rawCode = reader.getText(detPag, "tPag");
        SefazPaymentCode sefazCode = SefazPaymentCode.fromCode(rawCode);
        Element card = reader.findChild(detPag, "card").orElse(null);

        return InvoicePaymentData.builder()
                .paymentIndicator(reader.getInteger(detPag, "indPag"))
                .sefazCode(rawCode)
                .sefazPaymentCode(sefazCode)
                .paymentMethod(sefazCode.getErpPaymentMethod())
                .description(reader.getText(detPag, "xPag"))
                .amount(reader.getBigDecimalOrZero(detPag, "vPag"))
                .cardIntegrationType(card != null ? reader.getInteger(card, "tpIntegra") : null)
                .cardAcquirerCnpj(card != null ? reader.digitsOnly(reader.getText(card, "CNPJ")) : null)
                .cardBrandCode(card != null ? reader.getText(card, "tBand") : null)
                .authorizationCode(card != null ? reader.getText(card, "cAut") : null)
                .build();
    }
}
