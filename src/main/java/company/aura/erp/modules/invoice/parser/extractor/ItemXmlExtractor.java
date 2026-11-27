package company.aura.erp.modules.invoice.parser.extractor;

import company.aura.erp.modules.invoice.domain.InvoiceItemData;
import company.aura.erp.modules.invoice.domain.InvoiceItemTaxData;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.InvoiceXmlSectionExtractor;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class ItemXmlExtractor implements InvoiceXmlSectionExtractor<List<InvoiceItemData>> {

    @Override
    public List<InvoiceItemData> extract(InvoiceXmlContext context) {
        log.info("extract() - START - Extracting invoice items (<det>)");
        XmlElementReader reader = context.getReader();
        List<Element> detElements = reader.findChildren(context.getInfNFeElement(), "det");

        List<InvoiceItemData> items = detElements.stream()
                .map(det -> extractSingleItem(reader, det))
                .toList();

        log.info("extract() - END - totalItems={}", items.size());
        return items;
    }

    private InvoiceItemData extractSingleItem(XmlElementReader reader, Element det) {
        Integer itemNumber = parseItemNumber(reader.getAttribute(det, "nItem"));
        Element prod = reader.findChild(det, "prod").orElse(null);
        Element imposto = reader.findChild(det, "imposto").orElse(null);

        if (prod == null) {
            log.warn("extractSingleItem() - WARN - Tag <prod> not found for itemNumber={}", itemNumber);
            return InvoiceItemData.builder()
                    .itemNumber(itemNumber)
                    .additionalInfo(reader.getText(det, "infAdProd"))
                    .build();
        }

        BigDecimal totalPrice = reader.getBigDecimalOrZero(prod, "vProd");
        BigDecimal freightAmount = reader.getBigDecimalOrZero(prod, "vFrete");
        BigDecimal insuranceAmount = reader.getBigDecimalOrZero(prod, "vSeg");
        BigDecimal discountAmount = reader.getBigDecimalOrZero(prod, "vDesc");
        BigDecimal otherExpensesAmount = reader.getBigDecimalOrZero(prod, "vOutro");

        BigDecimal netTotalPrice = totalPrice
                .subtract(discountAmount)
                .add(freightAmount)
                .add(insuranceAmount)
                .add(otherExpensesAmount);

        return InvoiceItemData.builder()
                .itemNumber(itemNumber)
                .productCode(reader.getText(prod, "cProd"))
                .gtinEan(reader.getText(prod, "cEAN"))
                .taxableGtinEan(reader.getText(prod, "cEANTrib"))
                .description(reader.getText(prod, "xProd"))
                .ncm(reader.getText(prod, "NCM"))
                .cest(reader.getText(prod, "CEST"))
                .cfop(reader.getText(prod, "CFOP"))
                .unitOfMeasure(reader.getText(prod, "uCom"))
                .quantity(reader.getBigDecimalOrZero(prod, "qCom"))
                .unitPrice(reader.getBigDecimalOrZero(prod, "vUnCom"))
                .totalPrice(totalPrice)
                .taxableUnitOfMeasure(reader.getText(prod, "uTrib"))
                .taxableQuantity(reader.getBigDecimalOrZero(prod, "qTrib"))
                .taxableUnitPrice(reader.getBigDecimalOrZero(prod, "vUnTrib"))
                .freightAmount(freightAmount)
                .insuranceAmount(insuranceAmount)
                .discountAmount(discountAmount)
                .otherExpensesAmount(otherExpensesAmount)
                .netTotalPrice(netTotalPrice)
                .totalIndicator(reader.getInteger(prod, "indTot"))
                .purchaseOrderNumber(reader.getText(prod, "xPed"))
                .purchaseOrderItemNumber(reader.getInteger(prod, "nItemPed"))
                .additionalInfo(reader.getText(det, "infAdProd"))
                .taxes(extractTaxes(reader, imposto))
                .build();
    }

    private InvoiceItemTaxData extractTaxes(XmlElementReader reader, Element imposto) {
        if (imposto == null) {
            return InvoiceItemTaxData.builder().build();
        }

        InvoiceItemTaxData.InvoiceItemTaxDataBuilder taxBuilder = InvoiceItemTaxData.builder()
                .totalTaxesAmount(reader.getBigDecimalOrZero(imposto, "vTotTrib"));

        populateIcms(reader, imposto, taxBuilder);
        populateIpi(reader, imposto, taxBuilder);
        populatePis(reader, imposto, taxBuilder);
        populateCofins(reader, imposto, taxBuilder);

        return taxBuilder.build();
    }

    private void populateIcms(XmlElementReader reader, Element imposto, InvoiceItemTaxData.InvoiceItemTaxDataBuilder builder) {
        Element icmsGroup = reader.findChild(imposto, "ICMS").orElse(null);
        Element icmsDetail = icmsGroup != null ? reader.findFirstChildElement(icmsGroup).orElse(null) : null;
        if (icmsDetail == null) {
            return;
        }

        String csosn = reader.getText(icmsDetail, "CSOSN");
        String cst = reader.getText(icmsDetail, "CST");

        builder.origin(reader.getInteger(icmsDetail, "orig"))
                .icmsCstOrCsosn(csosn != null ? csosn : cst)
                .icmsBaseAmount(reader.getBigDecimalOrZero(icmsDetail, "vBC"))
                .icmsRate(reader.getBigDecimalOrZero(icmsDetail, "pICMS"))
                .icmsAmount(reader.getBigDecimalOrZero(icmsDetail, "vICMS"))
                .icmsCreditRate(reader.getBigDecimalOrZero(icmsDetail, "pCredSN"))
                .icmsCreditAmount(reader.getBigDecimalOrZero(icmsDetail, "vCredICMSSN"))
                .icmsStBaseAmount(reader.getBigDecimalOrZero(icmsDetail, "vBCST"))
                .icmsStAmount(reader.getBigDecimalOrZero(icmsDetail, "vICMSST"));
    }

    private void populateIpi(XmlElementReader reader, Element imposto, InvoiceItemTaxData.InvoiceItemTaxDataBuilder builder) {
        Element ipiGroup = reader.findChild(imposto, "IPI").orElse(null);
        if (ipiGroup == null) {
            return;
        }
        Element ipiDetail = reader.findChild(ipiGroup, "IPITrib")
                .or(() -> reader.findChild(ipiGroup, "IPINT"))
                .orElse(null);
        if (ipiDetail == null) {
            return;
        }

        builder.ipiCst(reader.getText(ipiDetail, "CST"))
                .ipiBaseAmount(reader.getBigDecimalOrZero(ipiDetail, "vBC"))
                .ipiRate(reader.getBigDecimalOrZero(ipiDetail, "pIPI"))
                .ipiAmount(reader.getBigDecimalOrZero(ipiDetail, "vIPI"));
    }

    private void populatePis(XmlElementReader reader, Element imposto, InvoiceItemTaxData.InvoiceItemTaxDataBuilder builder) {
        Element pisGroup = reader.findChild(imposto, "PIS").orElse(null);
        Element pisDetail = pisGroup != null ? reader.findFirstChildElement(pisGroup).orElse(null) : null;
        if (pisDetail == null) {
            return;
        }

        builder.pisCst(reader.getText(pisDetail, "CST"))
                .pisBaseAmount(reader.getBigDecimalOrZero(pisDetail, "vBC"))
                .pisRate(reader.getBigDecimalOrZero(pisDetail, "pPIS"))
                .pisAmount(reader.getBigDecimalOrZero(pisDetail, "vPIS"));
    }

    private void populateCofins(XmlElementReader reader, Element imposto, InvoiceItemTaxData.InvoiceItemTaxDataBuilder builder) {
        Element cofinsGroup = reader.findChild(imposto, "COFINS").orElse(null);
        Element cofinsDetail = cofinsGroup != null ? reader.findFirstChildElement(cofinsGroup).orElse(null) : null;
        if (cofinsDetail == null) {
            return;
        }

        builder.cofinsCst(reader.getText(cofinsDetail, "CST"))
                .cofinsBaseAmount(reader.getBigDecimalOrZero(cofinsDetail, "vBC"))
                .cofinsRate(reader.getBigDecimalOrZero(cofinsDetail, "pCOFINS"))
                .cofinsAmount(reader.getBigDecimalOrZero(cofinsDetail, "vCOFINS"));
    }

    private Integer parseItemNumber(String rawItemNumber) {
        if (rawItemNumber == null) {
            return null;
        }
        try {
            return Integer.valueOf(rawItemNumber);
        } catch (NumberFormatException ex) {
            log.warn("parseItemNumber() - WARN - Invalid nItem attribute value={}", rawItemNumber);
            return null;
        }
    }
}
