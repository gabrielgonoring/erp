package company.aura.erp.modules.invoice.parser.extractor;

import company.aura.erp.modules.invoice.domain.InvoiceTotalsData;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.InvoiceXmlSectionExtractor;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

@Slf4j
@Component
public class TotalsXmlExtractor implements InvoiceXmlSectionExtractor<InvoiceTotalsData> {

    @Override
    public InvoiceTotalsData extract(InvoiceXmlContext context) {
        log.info("extract() - START - Extracting invoice totals (<total>/<ICMSTot>)");
        XmlElementReader reader = context.getReader();
        Element totalElement = reader.findChild(context.getInfNFeElement(), "total").orElse(null);
        Element icmsTot = totalElement != null ? reader.findChild(totalElement, "ICMSTot").orElse(null) : null;

        if (icmsTot == null) {
            log.warn("extract() - WARN - Tag <ICMSTot> not found in <total>, returning zeroed totals");
            return InvoiceTotalsData.builder().build();
        }

        InvoiceTotalsData totals = InvoiceTotalsData.builder()
                .icmsBaseAmount(reader.getBigDecimalOrZero(icmsTot, "vBC"))
                .icmsAmount(reader.getBigDecimalOrZero(icmsTot, "vICMS"))
                .icmsExemptAmount(reader.getBigDecimalOrZero(icmsTot, "vICMSDeson"))
                .fcpAmount(reader.getBigDecimalOrZero(icmsTot, "vFCP"))
                .icmsStBaseAmount(reader.getBigDecimalOrZero(icmsTot, "vBCST"))
                .icmsStAmount(reader.getBigDecimalOrZero(icmsTot, "vST"))
                .productsAmount(reader.getBigDecimalOrZero(icmsTot, "vProd"))
                .freightAmount(reader.getBigDecimalOrZero(icmsTot, "vFrete"))
                .insuranceAmount(reader.getBigDecimalOrZero(icmsTot, "vSeg"))
                .discountAmount(reader.getBigDecimalOrZero(icmsTot, "vDesc"))
                .importTaxAmount(reader.getBigDecimalOrZero(icmsTot, "vII"))
                .ipiAmount(reader.getBigDecimalOrZero(icmsTot, "vIPI"))
                .ipiReturnedAmount(reader.getBigDecimalOrZero(icmsTot, "vIPIDevol"))
                .pisAmount(reader.getBigDecimalOrZero(icmsTot, "vPIS"))
                .cofinsAmount(reader.getBigDecimalOrZero(icmsTot, "vCOFINS"))
                .additionalExpenses(reader.getBigDecimalOrZero(icmsTot, "vOutro"))
                .totalAmount(reader.getBigDecimalOrZero(icmsTot, "vNF"))
                .totalTaxesAmount(reader.getBigDecimalOrZero(icmsTot, "vTotTrib"))
                .build();

        log.info("extract() - END - productsAmount={}, freightAmount={}, discountAmount={}, totalAmount={}",
                totals.getProductsAmount(), totals.getFreightAmount(), totals.getDiscountAmount(), totals.getTotalAmount());
        return totals;
    }
}
