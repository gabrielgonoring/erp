package company.aura.erp.modules.invoice.parser.extractor;

import company.aura.erp.enums.EnvironmentType;
import company.aura.erp.modules.invoice.domain.InvoiceProtocolData;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.InvoiceXmlSectionExtractor;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.util.Set;

@Slf4j
@Component
public class ProtocolXmlExtractor implements InvoiceXmlSectionExtractor<InvoiceProtocolData> {

    private static final Set<String> AUTHORIZED_STATUS_CODES = Set.of("100", "150");

    @Override
    public InvoiceProtocolData extract(InvoiceXmlContext context) {
        log.info("extract() - START - Extracting authorization protocol");
        if (context.getProtNFeElement() == null) {
            log.info("extract() - END - Tag <protNFe> absent, returning null protocol");
            return null;
        }

        XmlElementReader reader = context.getReader();
        Element infProt = reader.findChild(context.getProtNFeElement(), "infProt").orElse(null);
        if (infProt == null) {
            log.warn("extract() - WARN - Tag <infProt> not found inside <protNFe>");
            return null;
        }

        Integer tpAmb = reader.getInteger(infProt, "tpAmb");
        String statusCode = reader.getText(infProt, "cStat");

        InvoiceProtocolData protocol = InvoiceProtocolData.builder()
                .environment(tpAmb != null ? (tpAmb == 1 ? EnvironmentType.PRD : EnvironmentType.STG) : null)
                .applicationVersion(reader.getText(infProt, "verAplic"))
                .accessKey(reader.digitsOnly(reader.getText(infProt, "chNFe")))
                .authorizedAt(reader.getLocalDateTime(infProt, "dhRecbto"))
                .authorizationProtocol(reader.getText(infProt, "nProt"))
                .digestValue(reader.getText(infProt, "digVal"))
                .statusCode(statusCode)
                .statusReason(reader.getText(infProt, "xMotivo"))
                .authorized(statusCode != null && AUTHORIZED_STATUS_CODES.contains(statusCode))
                .build();

        log.info("extract() - END - protocol={}, statusCode={}, authorized={}",
                protocol.getAuthorizationProtocol(), protocol.getStatusCode(), protocol.isAuthorized());
        return protocol;
    }
}
