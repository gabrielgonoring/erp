package company.aura.erp.modules.invoice.parser.extractor;

import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import company.aura.erp.modules.invoice.domain.InvoiceIdentificationData;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.InvoiceXmlSectionExtractor;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class IdentificationXmlExtractor implements InvoiceXmlSectionExtractor<InvoiceIdentificationData> {

    @Override
    public InvoiceIdentificationData extract(InvoiceXmlContext context) {
        log.info("extract() - START - Extracting invoice identification");
        XmlElementReader reader = context.getReader();
        Element infNFe = context.getInfNFeElement();
        Element ide = reader.findChild(infNFe, "ide").orElse(null);

        String accessKey = resolveAccessKey(context, reader, infNFe);
        if (ide == null) {
            log.warn("extract() - WARN - Tag <ide> not found in <infNFe>");
            return InvoiceIdentificationData.builder()
                    .accessKey(accessKey)
                    .build();
        }

        String modelCode = reader.getText(ide, "mod");
        DocumentType documentType = resolveDocumentType(modelCode);
        EnvironmentType environment = resolveEnvironment(reader.getInteger(ide, "tpAmb"));
        LocalDateTime issuedAt = resolveIssuedAt(reader, ide);
        LocalDateTime exitOrEntryAt = reader.getLocalDateTime(ide, "dhSaiEnt");
        List<String> referencedKeys = extractReferencedAccessKeys(reader, ide);

        InvoiceIdentificationData data = InvoiceIdentificationData.builder()
                .accessKey(accessKey)
                .modelCode(modelCode)
                .documentType(documentType)
                .environment(environment)
                .invoiceNumber(reader.getInteger(ide, "nNF"))
                .series(reader.getInteger(ide, "serie"))
                .numericCode(reader.getText(ide, "cNF"))
                .operationType(reader.getInteger(ide, "tpNF"))
                .operationNature(reader.getText(ide, "natOp"))
                .destinationIndicator(reader.getInteger(ide, "idDest"))
                .purposeCode(reader.getInteger(ide, "finNFe"))
                .finalConsumerIndicator(reader.getInteger(ide, "indFinal"))
                .presenceIndicator(reader.getInteger(ide, "indPres"))
                .emissionType(reader.getInteger(ide, "tpEmis"))
                .printFormat(reader.getInteger(ide, "tpImp"))
                .issuingCityIbgeCode(reader.getText(ide, "cMunFG"))
                .issuedAt(issuedAt)
                .exitOrEntryAt(exitOrEntryAt)
                .referencedAccessKeys(referencedKeys)
                .build();

        log.info("extract() - END - accessKey={}, documentType={}, invoiceNumber={}, series={}",
                data.getAccessKey(), data.getDocumentType(), data.getInvoiceNumber(), data.getSeries());
        return data;
    }

    private String resolveAccessKey(InvoiceXmlContext context, XmlElementReader reader, Element infNFe) {
        String rawId = reader.getAttribute(infNFe, "Id");
        if (StringUtils.isNotBlank(rawId)) {
            String digits = reader.digitsOnly(rawId);
            if (StringUtils.isNotBlank(digits)) {
                return digits;
            }
        }
        if (context.getProtNFeElement() != null) {
            return reader.findChild(context.getProtNFeElement(), "infProt")
                    .map(infProt -> reader.digitsOnly(reader.getText(infProt, "chNFe")))
                    .orElse(null);
        }
        return null;
    }

    private DocumentType resolveDocumentType(String modelCode) {
        if ("65".equals(modelCode)) {
            return DocumentType.NFCE;
        }
        if ("55".equals(modelCode)) {
            return DocumentType.NFE;
        }
        return DocumentType.NFE;
    }

    private EnvironmentType resolveEnvironment(Integer tpAmb) {
        if (tpAmb == null) {
            return null;
        }
        return tpAmb == 1 ? EnvironmentType.PRD : EnvironmentType.STG;
    }

    private LocalDateTime resolveIssuedAt(XmlElementReader reader, Element ide) {
        LocalDateTime dhEmi = reader.getLocalDateTime(ide, "dhEmi");
        if (dhEmi != null) {
            return dhEmi;
        }
        return reader.getLocalDateTime(ide, "dEmi");
    }

    private List<String> extractReferencedAccessKeys(XmlElementReader reader, Element ide) {
        return reader.findChildren(ide, "NFref").stream()
                .map(nfRef -> reader.digitsOnly(reader.getText(nfRef, "refNFe")))
                .filter(Objects::nonNull)
                .toList();
    }
}
