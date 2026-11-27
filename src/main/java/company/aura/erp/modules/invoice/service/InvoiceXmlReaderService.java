package company.aura.erp.modules.invoice.service;

import company.aura.erp.modules.invoice.domain.InvoiceAdditionalInfoData;
import company.aura.erp.modules.invoice.domain.InvoiceBillingData;
import company.aura.erp.modules.invoice.domain.InvoiceFreightData;
import company.aura.erp.modules.invoice.domain.InvoiceIdentificationData;
import company.aura.erp.modules.invoice.domain.InvoiceItemData;
import company.aura.erp.modules.invoice.domain.InvoicePartnerData;
import company.aura.erp.modules.invoice.domain.InvoiceProtocolData;
import company.aura.erp.modules.invoice.domain.InvoiceTotalsData;
import company.aura.erp.modules.invoice.domain.ParsedInvoiceXml;
import company.aura.erp.modules.invoice.exception.InvoiceXmlParseException;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.SecureXmlDocumentFactory;
import company.aura.erp.modules.invoice.parser.extractor.AdditionalInfoXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.BillingXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.FreightXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.IdentificationXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.ItemXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.PartnerXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.ProtocolXmlExtractor;
import company.aura.erp.modules.invoice.parser.extractor.TotalsXmlExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceXmlReaderService {

    private final SecureXmlDocumentFactory xmlDocumentFactory;
    private final IdentificationXmlExtractor identificationExtractor;
    private final ProtocolXmlExtractor protocolExtractor;
    private final PartnerXmlExtractor partnerExtractor;
    private final ItemXmlExtractor itemExtractor;
    private final TotalsXmlExtractor totalsExtractor;
    private final FreightXmlExtractor freightExtractor;
    private final BillingXmlExtractor billingExtractor;
    private final AdditionalInfoXmlExtractor additionalInfoExtractor;

    public ParsedInvoiceXml read(MultipartFile xmlFile) {
        String fileName = xmlFile != null ? xmlFile.getOriginalFilename() : null;
        log.info("read() - START - source=MultipartFile, fileName={}", fileName);
        try {
            InvoiceXmlContext context = xmlDocumentFactory.createContext(xmlFile);
            ParsedInvoiceXml result = parseContext(context);
            log.info("read() - END - source=MultipartFile, accessKey={}, itemsCount={}",
                    extractSafeAccessKey(result), extractSafeItemsCount(result));
            return result;
        } catch (InvoiceXmlParseException ex) {
            log.error("read() - ERROR - Failed to read invoice XML from MultipartFile - fileName={}, error: {}", fileName, ex.getMessage());
            throw ex;
        }
    }

    public ParsedInvoiceXml read(byte[] xmlBytes) {
        int size = xmlBytes != null ? xmlBytes.length : 0;
        log.info("read() - START - source=byte[], size={}", size);
        try {
            InvoiceXmlContext context = xmlDocumentFactory.createContext(xmlBytes);
            ParsedInvoiceXml result = parseContext(context);
            log.info("read() - END - source=byte[], accessKey={}, itemsCount={}",
                    extractSafeAccessKey(result), extractSafeItemsCount(result));
            return result;
        } catch (InvoiceXmlParseException ex) {
            log.error("read() - ERROR - Failed to read invoice XML from byte[] - error: {}", ex.getMessage());
            throw ex;
        }
    }

    public ParsedInvoiceXml read(InputStream xmlStream) {
        log.info("read() - START - source=InputStream");
        try {
            InvoiceXmlContext context = xmlDocumentFactory.createContext(xmlStream);
            ParsedInvoiceXml result = parseContext(context);
            log.info("read() - END - source=InputStream, accessKey={}, itemsCount={}",
                    extractSafeAccessKey(result), extractSafeItemsCount(result));
            return result;
        } catch (InvoiceXmlParseException ex) {
            log.error("read() - ERROR - Failed to read invoice XML from InputStream - error: {}", ex.getMessage());
            throw ex;
        }
    }

    public ParsedInvoiceXml read(String xmlContent) {
        int length = xmlContent != null ? xmlContent.length() : 0;
        log.info("read() - START - source=String, length={}", length);
        try {
            InvoiceXmlContext context = xmlDocumentFactory.createContext(xmlContent);
            ParsedInvoiceXml result = parseContext(context);
            log.info("read() - END - source=String, accessKey={}, itemsCount={}",
                    extractSafeAccessKey(result), extractSafeItemsCount(result));
            return result;
        } catch (InvoiceXmlParseException ex) {
            log.error("read() - ERROR - Failed to read invoice XML from String - error: {}", ex.getMessage());
            throw ex;
        }
    }

    public InvoiceIdentificationData readIdentification(String xmlContent) {
        log.info("readIdentification() - START");
        InvoiceIdentificationData data = readIdentification(xmlDocumentFactory.createContext(xmlContent));
        log.info("readIdentification() - END - accessKey={}", data.getAccessKey());
        return data;
    }

    public InvoiceProtocolData readProtocol(String xmlContent) {
        log.info("readProtocol() - START");
        InvoiceProtocolData data = readProtocol(xmlDocumentFactory.createContext(xmlContent));
        log.info("readProtocol() - END - hasProtocol={}", data != null);
        return data;
    }

    public InvoicePartnerData readEmitter(String xmlContent) {
        log.info("readEmitter() - START");
        InvoicePartnerData data = readEmitter(xmlDocumentFactory.createContext(xmlContent));
        log.info("readEmitter() - END - document={}", data != null ? data.getDocument() : null);
        return data;
    }

    public InvoicePartnerData readRecipient(String xmlContent) {
        log.info("readRecipient() - START");
        InvoicePartnerData data = readRecipient(xmlDocumentFactory.createContext(xmlContent));
        log.info("readRecipient() - END - document={}", data != null ? data.getDocument() : null);
        return data;
    }

    public InvoicePartnerData readPartner(String xmlContent) {
        log.info("readPartner() - START");
        InvoiceXmlContext context = xmlDocumentFactory.createContext(xmlContent);
        InvoiceIdentificationData identification = readIdentification(context);
        InvoicePartnerData emitter = readEmitter(context);
        InvoicePartnerData recipient = readRecipient(context);
        InvoicePartnerData partner = resolvePrimaryPartner(emitter, recipient, identification.getOperationType());
        log.info("readPartner() - END - document={}", partner != null ? partner.getDocument() : null);
        return partner;
    }

    public List<InvoiceItemData> readItems(String xmlContent) {
        log.info("readItems() - START");
        List<InvoiceItemData> items = readItems(xmlDocumentFactory.createContext(xmlContent));
        log.info("readItems() - END - totalItems={}", items.size());
        return items;
    }

    public InvoiceTotalsData readTotals(String xmlContent) {
        log.info("readTotals() - START");
        InvoiceTotalsData totals = readTotals(xmlDocumentFactory.createContext(xmlContent));
        log.info("readTotals() - END - totalAmount={}", totals.getTotalAmount());
        return totals;
    }

    public InvoiceFreightData readFreight(String xmlContent) {
        log.info("readFreight() - START");
        InvoiceFreightData freight = readFreight(xmlDocumentFactory.createContext(xmlContent));
        log.info("readFreight() - END - freightMode={}", freight.getFreightMode());
        return freight;
    }

    public InvoiceBillingData readBilling(String xmlContent) {
        log.info("readBilling() - START");
        InvoiceBillingData billing = readBilling(xmlDocumentFactory.createContext(xmlContent));
        log.info("readBilling() - END - installments={}, payments={}",
                billing.getInstallments().size(), billing.getPayments().size());
        return billing;
    }

    public InvoiceAdditionalInfoData readAdditionalInfo(String xmlContent) {
        log.info("readAdditionalInfo() - START");
        InvoiceAdditionalInfoData info = readAdditionalInfo(xmlDocumentFactory.createContext(xmlContent));
        log.info("readAdditionalInfo() - END");
        return info;
    }

    public InvoicePartnerData resolveCounterpartyPartner(ParsedInvoiceXml parsedInvoice, String ownCompanyCnpj) {
        log.info("resolveCounterpartyPartner() - START - ownCompanyCnpj={}", ownCompanyCnpj);
        if (parsedInvoice == null) {
            log.warn("resolveCounterpartyPartner() - WARN - ParsedInvoiceXml is null");
            return null;
        }
        String normalizedCompanyDoc = ownCompanyCnpj != null ? ownCompanyCnpj.replaceAll("\\D", "") : null;
        InvoicePartnerData emitter = parsedInvoice.getEmitter();
        InvoicePartnerData recipient = parsedInvoice.getRecipient();

        if (StringUtils.isNotBlank(normalizedCompanyDoc)) {
            if (emitter != null && normalizedCompanyDoc.equals(emitter.getDocument())) {
                log.info("resolveCounterpartyPartner() - END - Company is emitter, returning recipient={}",
                        recipient != null ? recipient.getDocument() : null);
                return recipient;
            }
            if (recipient != null && normalizedCompanyDoc.equals(recipient.getDocument())) {
                log.info("resolveCounterpartyPartner() - END - Company is recipient, returning emitter={}",
                        emitter != null ? emitter.getDocument() : null);
                return emitter;
            }
        }

        InvoicePartnerData fallback = parsedInvoice.getPartner();
        log.info("resolveCounterpartyPartner() - END - Fallback to default partner={}",
                fallback != null ? fallback.getDocument() : null);
        return fallback;
    }

    protected ParsedInvoiceXml parseContext(InvoiceXmlContext context) {
        log.info("parseContext() - START");

        InvoiceIdentificationData identification = readIdentification(context);
        InvoiceProtocolData protocol = readProtocol(context);
        InvoicePartnerData emitter = readEmitter(context);
        InvoicePartnerData recipient = readRecipient(context);
        InvoicePartnerData primaryPartner = resolvePrimaryPartner(emitter, recipient, identification.getOperationType());
        List<InvoiceItemData> items = readItems(context);
        InvoiceTotalsData totals = readTotals(context);
        InvoiceFreightData freight = readFreight(context);
        InvoiceBillingData billing = readBilling(context);
        InvoiceAdditionalInfoData additionalInfo = readAdditionalInfo(context);

        ParsedInvoiceXml parsed = ParsedInvoiceXml.builder()
                .identification(identification)
                .protocol(protocol)
                .emitter(emitter)
                .recipient(recipient)
                .partner(primaryPartner)
                .items(items)
                .totals(totals)
                .freight(freight)
                .billing(billing)
                .additionalInfo(additionalInfo)
                .build();

        log.info("parseContext() - END - accessKey={}, invoiceNumber={}, totalAmount={}",
                identification.getAccessKey(),
                identification.getInvoiceNumber(),
                totals.getTotalAmount());
        return parsed;
    }

    protected InvoiceIdentificationData readIdentification(InvoiceXmlContext context) {
        return identificationExtractor.extract(context);
    }

    protected InvoiceProtocolData readProtocol(InvoiceXmlContext context) {
        return protocolExtractor.extract(context);
    }

    protected InvoicePartnerData readEmitter(InvoiceXmlContext context) {
        return partnerExtractor.extractEmitter(context);
    }

    protected InvoicePartnerData readRecipient(InvoiceXmlContext context) {
        return partnerExtractor.extractRecipient(context);
    }

    protected InvoicePartnerData resolvePrimaryPartner(InvoicePartnerData emitter, InvoicePartnerData recipient, Integer operationType) {
        return partnerExtractor.resolvePrimaryPartner(emitter, recipient, operationType);
    }

    protected List<InvoiceItemData> readItems(InvoiceXmlContext context) {
        return itemExtractor.extract(context);
    }

    protected InvoiceTotalsData readTotals(InvoiceXmlContext context) {
        return totalsExtractor.extract(context);
    }

    protected InvoiceFreightData readFreight(InvoiceXmlContext context) {
        return freightExtractor.extract(context);
    }

    protected InvoiceBillingData readBilling(InvoiceXmlContext context) {
        return billingExtractor.extract(context);
    }

    protected InvoiceAdditionalInfoData readAdditionalInfo(InvoiceXmlContext context) {
        return additionalInfoExtractor.extract(context);
    }

    private String extractSafeAccessKey(ParsedInvoiceXml result) {
        return (result != null && result.getIdentification() != null)
                ? result.getIdentification().getAccessKey()
                : null;
    }

    private int extractSafeItemsCount(ParsedInvoiceXml result) {
        return (result != null && result.getItems() != null)
                ? result.getItems().size()
                : 0;
    }
}
