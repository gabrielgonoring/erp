package company.aura.erp.modules.invoice.parser.extractor;

import company.aura.erp.modules.invoice.domain.InvoiceAdditionalInfoData;
import company.aura.erp.modules.invoice.domain.InvoiceObservationData;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.InvoiceXmlSectionExtractor;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.util.List;

@Slf4j
@Component
public class AdditionalInfoXmlExtractor implements InvoiceXmlSectionExtractor<InvoiceAdditionalInfoData> {

    @Override
    public InvoiceAdditionalInfoData extract(InvoiceXmlContext context) {
        log.info("extract() - START - Extracting additional information (<infAdic>)");
        XmlElementReader reader = context.getReader();
        Element infAdic = reader.findChild(context.getInfNFeElement(), "infAdic").orElse(null);

        if (infAdic == null) {
            log.info("extract() - END - Tag <infAdic> absent, returning empty additional info");
            return InvoiceAdditionalInfoData.builder().build();
        }

        List<InvoiceObservationData> taxpayerObservations = extractObservations(reader, infAdic, "obsCont");
        List<InvoiceObservationData> fiscoObservations = extractObservations(reader, infAdic, "obsFisco");

        InvoiceAdditionalInfoData additionalInfo = InvoiceAdditionalInfoData.builder()
                .fiscoInfo(reader.getText(infAdic, "infAdFisco"))
                .taxpayerInfo(reader.getText(infAdic, "infCpl"))
                .taxpayerObservations(taxpayerObservations)
                .fiscoObservations(fiscoObservations)
                .build();

        log.info("extract() - END - hasTaxpayerInfo={}, taxpayerObsCount={}, fiscoObsCount={}",
                additionalInfo.getTaxpayerInfo() != null, taxpayerObservations.size(), fiscoObservations.size());
        return additionalInfo;
    }

    private List<InvoiceObservationData> extractObservations(XmlElementReader reader, Element infAdic, String tagName) {
        return reader.findChildren(infAdic, tagName).stream()
                .map(obs -> InvoiceObservationData.builder()
                        .field(reader.getAttribute(obs, "xCampo"))
                        .text(reader.getText(obs, "xTexto"))
                        .build())
                .toList();
    }
}
