package company.aura.erp.modules.invoice.parser.extractor;

import company.aura.erp.enums.PersonType;
import company.aura.erp.modules.invoice.domain.InvoiceCarrierData;
import company.aura.erp.modules.invoice.domain.InvoiceFreightData;
import company.aura.erp.modules.invoice.domain.InvoiceVolumeData;
import company.aura.erp.modules.invoice.enumerate.FreightMode;
import company.aura.erp.modules.invoice.parser.InvoiceXmlContext;
import company.aura.erp.modules.invoice.parser.InvoiceXmlSectionExtractor;
import company.aura.erp.modules.invoice.parser.XmlElementReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import java.util.List;

@Slf4j
@Component
public class FreightXmlExtractor implements InvoiceXmlSectionExtractor<InvoiceFreightData> {

    @Override
    public InvoiceFreightData extract(InvoiceXmlContext context) {
        log.info("extract() - START - Extracting freight/transport data (<transp>)");
        XmlElementReader reader = context.getReader();
        Element transp = reader.findChild(context.getInfNFeElement(), "transp").orElse(null);

        if (transp == null) {
            log.info("extract() - END - Tag <transp> absent, returning default NO_FREIGHT");
            return InvoiceFreightData.builder().build();
        }

        Integer rawMode = reader.getInteger(transp, "modFrete");
        FreightMode freightMode = FreightMode.fromCode(rawMode);
        InvoiceCarrierData carrier = extractCarrier(reader, transp);
        Element vehicle = reader.findChild(transp, "veicTransp").orElse(null);
        List<InvoiceVolumeData> volumes = extractVolumes(reader, transp);

        InvoiceFreightData freight = InvoiceFreightData.builder()
                .freightModeCode(freightMode.getCode())
                .freightMode(freightMode)
                .carrier(carrier)
                .vehiclePlate(vehicle != null ? reader.getText(vehicle, "placa") : null)
                .vehicleState(vehicle != null ? reader.getText(vehicle, "UF") : null)
                .rntrcCode(vehicle != null ? reader.getText(vehicle, "RNTC") : null)
                .volumes(volumes)
                .build();

        log.info("extract() - END - freightMode={}, hasCarrier={}, totalVolumes={}",
                freight.getFreightMode(), carrier != null, volumes.size());
        return freight;
    }

    private InvoiceCarrierData extractCarrier(XmlElementReader reader, Element transp) {
        Element transporta = reader.findChild(transp, "transporta").orElse(null);
        if (transporta == null) {
            return null;
        }

        String cnpj = reader.digitsOnly(reader.getText(transporta, "CNPJ"));
        String cpf = reader.digitsOnly(reader.getText(transporta, "CPF"));
        PersonType personType = StringUtils.isNotBlank(cnpj)
                ? PersonType.PJ
                : (StringUtils.isNotBlank(cpf) ? PersonType.PF : null);

        return InvoiceCarrierData.builder()
                .personType(personType)
                .document(cnpj != null ? cnpj : cpf)
                .name(reader.getText(transporta, "xNome"))
                .stateRegistration(reader.getText(transporta, "IE"))
                .address(reader.getText(transporta, "xEnder"))
                .city(reader.getText(transporta, "xMun"))
                .state(reader.getText(transporta, "UF"))
                .build();
    }

    private List<InvoiceVolumeData> extractVolumes(XmlElementReader reader, Element transp) {
        return reader.findChildren(transp, "vol").stream()
                .map(vol -> InvoiceVolumeData.builder()
                        .quantity(reader.getInteger(vol, "qVol"))
                        .species(reader.getText(vol, "esp"))
                        .brand(reader.getText(vol, "marca"))
                        .numbering(reader.getText(vol, "nVol"))
                        .netWeightKg(reader.getBigDecimalOrZero(vol, "pesoL"))
                        .grossWeightKg(reader.getBigDecimalOrZero(vol, "pesoB"))
                        .build())
                .toList();
    }
}
