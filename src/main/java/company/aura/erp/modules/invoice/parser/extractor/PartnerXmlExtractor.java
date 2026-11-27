package company.aura.erp.modules.invoice.parser.extractor;

import company.aura.erp.enums.PersonType;
import company.aura.erp.modules.invoice.domain.InvoiceAddressData;
import company.aura.erp.modules.invoice.domain.InvoicePartnerData;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

@Slf4j
@Component
public class PartnerXmlExtractor {

    private static final int NON_TAXPAYER_IE_INDICATOR = 9;
    private static final int TAXPAYER_IE_INDICATOR = 1;

    public InvoicePartnerData extractEmitter(InvoiceXmlContext context) {
        log.info("extractEmitter() - START - Extracting emitter data (<emit>)");
        XmlElementReader reader = context.getReader();
        Element emit = reader.findChild(context.getInfNFeElement(), "emit").orElse(null);
        if (emit == null) {
            log.warn("extractEmitter() - WARN - Tag <emit> not found in <infNFe>");
            return null;
        }

        InvoiceAddressData address = extractAddress(reader, emit, "enderEmit");
        String stateRegistration = reader.getText(emit, "IE");

        InvoicePartnerData emitter = InvoicePartnerData.builder()
                .personType(resolvePersonType(reader, emit))
                .document(resolveDocument(reader, emit))
                .name(reader.getText(emit, "xNome"))
                .tradeName(reader.getText(emit, "xFant"))
                .indicatorIe(StringUtils.isNotBlank(stateRegistration) ? TAXPAYER_IE_INDICATOR : NON_TAXPAYER_IE_INDICATOR)
                .stateRegistration(stateRegistration)
                .municipalRegistration(reader.getText(emit, "IM"))
                .cnae(reader.getText(emit, "CNAE"))
                .taxRegime(reader.getInteger(emit, "CRT"))
                .phone(address != null ? address.getPhone() : null)
                .address(address)
                .build();

        log.info("extractEmitter() - END - document={}, name={}", emitter.getDocument(), emitter.getName());
        return emitter;
    }

    public InvoicePartnerData extractRecipient(InvoiceXmlContext context) {
        log.info("extractRecipient() - START - Extracting recipient data (<dest>)");
        XmlElementReader reader = context.getReader();
        Element dest = reader.findChild(context.getInfNFeElement(), "dest").orElse(null);
        if (dest == null) {
            log.warn("extractRecipient() - WARN - Tag <dest> not found in <infNFe>");
            return null;
        }

        InvoiceAddressData address = extractAddress(reader, dest, "enderDest");
        Integer indicatorIe = reader.getInteger(dest, "indIEDest");

        InvoicePartnerData recipient = InvoicePartnerData.builder()
                .personType(resolvePersonType(reader, dest))
                .document(resolveDocument(reader, dest))
                .foreignId(reader.getText(dest, "idEstrangeiro"))
                .name(reader.getText(dest, "xNome"))
                .indicatorIe(indicatorIe != null ? indicatorIe : NON_TAXPAYER_IE_INDICATOR)
                .stateRegistration(reader.getText(dest, "IE"))
                .municipalRegistration(reader.getText(dest, "IM"))
                .email(reader.getText(dest, "email"))
                .phone(address != null ? address.getPhone() : null)
                .address(address)
                .build();

        log.info("extractRecipient() - END - document={}, name={}", recipient.getDocument(), recipient.getName());
        return recipient;
    }

    public InvoicePartnerData resolvePrimaryPartner(InvoicePartnerData emitter, InvoicePartnerData recipient, Integer operationType) {
        log.info("resolvePrimaryPartner() - START - operationType={}, hasEmitter={}, hasRecipient={}",
                operationType, emitter != null, recipient != null);

        InvoicePartnerData resolved;
        if (Integer.valueOf(0).equals(operationType)) {
            resolved = emitter != null ? emitter : recipient;
        } else {
            resolved = recipient != null ? recipient : emitter;
        }

        log.info("resolvePrimaryPartner() - END - resolvedDocument={}", resolved != null ? resolved.getDocument() : null);
        return resolved;
    }

    private PersonType resolvePersonType(XmlElementReader reader, Element partnerElement) {
        if (StringUtils.isNotBlank(reader.getText(partnerElement, "CNPJ"))) {
            return PersonType.PJ;
        }
        if (StringUtils.isNotBlank(reader.getText(partnerElement, "CPF"))) {
            return PersonType.PF;
        }
        return PersonType.PJ;
    }

    private String resolveDocument(XmlElementReader reader, Element partnerElement) {
        String cnpj = reader.digitsOnly(reader.getText(partnerElement, "CNPJ"));
        if (cnpj != null) {
            return cnpj;
        }
        return reader.digitsOnly(reader.getText(partnerElement, "CPF"));
    }

    private InvoiceAddressData extractAddress(XmlElementReader reader, Element partnerElement, String addressTag) {
        Element addr = reader.findChild(partnerElement, addressTag).orElse(null);
        if (addr == null) {
            return null;
        }
        return InvoiceAddressData.builder()
                .street(reader.getText(addr, "xLgr"))
                .number(reader.getText(addr, "nro"))
                .complement(reader.getText(addr, "xCpl"))
                .neighborhood(reader.getText(addr, "xBairro"))
                .ibgeCityCode(reader.digitsOnly(reader.getText(addr, "cMun")))
                .city(reader.getText(addr, "xMun"))
                .state(reader.getText(addr, "UF"))
                .zipCode(reader.digitsOnly(reader.getText(addr, "CEP")))
                .countryCode(reader.getText(addr, "cPais"))
                .countryName(reader.getText(addr, "xPais"))
                .phone(reader.digitsOnly(reader.getText(addr, "fone")))
                .build();
    }
}
