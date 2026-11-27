package company.aura.erp.modules.invoice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedInvoiceXml {
    private InvoiceIdentificationData identification;
    private InvoiceProtocolData protocol;
    private InvoicePartnerData emitter;
    private InvoicePartnerData recipient;
    private InvoicePartnerData partner;

    @Builder.Default
    private List<InvoiceItemData> items = new ArrayList<>();

    private InvoiceTotalsData totals;
    private InvoiceFreightData freight;
    private InvoiceBillingData billing;
    private InvoiceAdditionalInfoData additionalInfo;
}
