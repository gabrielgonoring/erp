package company.aura.erp.modules.invoice.mapper;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.order.OrderShippingRequest;
import company.aura.erp.dto.partner.PartnerCreateRequest;
import company.aura.erp.enums.InvoiceStatus;
import company.aura.erp.modules.invoice.domain.InvoiceAddressData;
import company.aura.erp.modules.invoice.domain.InvoiceBillingData;
import company.aura.erp.modules.invoice.domain.InvoiceFreightData;
import company.aura.erp.modules.invoice.domain.InvoiceIdentificationData;
import company.aura.erp.modules.invoice.domain.InvoicePartnerData;
import company.aura.erp.modules.invoice.domain.InvoiceProtocolData;
import company.aura.erp.modules.invoice.domain.InvoiceTotalsData;
import company.aura.erp.modules.invoice.domain.InvoiceVolumeData;
import company.aura.erp.modules.invoice.domain.ParsedInvoiceXml;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class InvoiceXmlErpMapper {

    private InvoiceXmlErpMapper() {
    }

    public static InvoiceCreateRequest toInvoiceCreateRequest(ParsedInvoiceXml parsed, Long partnerId, String externalReference) {
        if (parsed == null) {
            return null;
        }
        InvoiceIdentificationData id = parsed.getIdentification();
        InvoiceProtocolData protocol = parsed.getProtocol();
        InvoiceTotalsData totals = parsed.getTotals();
        InvoiceFreightData freight = parsed.getFreight();
        InvoiceBillingData billing = parsed.getBilling();

        String paymentMethod = resolvePaymentMethod(billing);
        LocalDate dueDate = resolveFirstDueDate(billing);
        String additionalInfo = parsed.getAdditionalInfo() != null
                ? parsed.getAdditionalInfo().getTaxpayerInfo()
                : null;

        return InvoiceCreateRequest.builder()
                .partnerId(partnerId)
                .externalReference(externalReference)
                .operationType(id != null ? id.getOperationType() : null)
                .documentType(id != null ? id.getDocumentType() : null)
                .environment(id != null ? id.getEnvironment() : null)
                .status(InvoiceStatus.ISSUED.name())
                .invoiceNumber(id != null ? id.getInvoiceNumber() : null)
                .series(id != null ? id.getSeries() : null)
                .accessKey(id != null ? id.getAccessKey() : null)
                .authorizationProtocol(protocol != null ? protocol.getAuthorizationProtocol() : null)
                .paymentMethod(paymentMethod)
                .dueDate(dueDate)
                .freightMode(freight != null ? freight.getFreightModeCode() : 9)
                .freightAmount(totals != null ? totals.getFreightAmount() : BigDecimal.ZERO)
                .additionalExpenses(totals != null ? totals.getAdditionalExpenses() : BigDecimal.ZERO)
                .productsAmount(totals != null ? totals.getProductsAmount() : BigDecimal.ZERO)
                .discountAmount(totals != null ? totals.getDiscountAmount() : BigDecimal.ZERO)
                .totalAmount(totals != null ? totals.getTotalAmount() : BigDecimal.ZERO)
                .additionalInformation(additionalInfo)
                .issuedAt(id != null ? id.getIssuedAt() : null)
                .build();
    }

    public static PartnerCreateRequest toPartnerCreateRequest(
            InvoicePartnerData partnerData,
            boolean isCustomer,
            boolean isSupplier,
            boolean isCarrier
    ) {
        if (partnerData == null) {
            return null;
        }
        InvoiceAddressData addr = partnerData.getAddress();
        return new PartnerCreateRequest(
                isCustomer,
                isSupplier,
                isCarrier,
                partnerData.getPersonType(),
                partnerData.getDocument(),
                partnerData.getName(),
                partnerData.getIndicatorIe(),
                partnerData.getStateRegistration(),
                partnerData.getEmail(),
                partnerData.getPhone(),
                addr != null ? addr.getStreet() : null,
                addr != null ? addr.getNumber() : null,
                addr != null ? addr.getComplement() : null,
                addr != null ? addr.getNeighborhood() : null,
                addr != null ? addr.getIbgeCityCode() : null,
                addr != null ? addr.getCity() : null,
                addr != null ? addr.getState() : null,
                addr != null ? addr.getZipCode() : null,
                addr != null && StringUtils.isNotBlank(addr.getCountryCode()) ? addr.getCountryCode() : "1058",
                addr != null && StringUtils.isNotBlank(addr.getCountryName()) ? addr.getCountryName() : "Brasil"
        );
    }

    public static OrderShippingRequest toOrderShippingRequest(ParsedInvoiceXml parsed, Long carrierId) {
        if (parsed == null || parsed.getFreight() == null) {
            return null;
        }
        InvoiceFreightData freight = parsed.getFreight();
        InvoiceVolumeData firstVolume = (freight.getVolumes() != null && !freight.getVolumes().isEmpty())
                ? freight.getVolumes().get(0)
                : null;
        BigDecimal freightAmount = parsed.getTotals() != null ? parsed.getTotals().getFreightAmount() : BigDecimal.ZERO;

        return OrderShippingRequest.builder()
                .freightMode(freight.getFreightModeCode())
                .carrierId(carrierId)
                .vehiclePlate(freight.getVehiclePlate())
                .vehicleState(freight.getVehicleState())
                .rntrcCode(freight.getRntrcCode())
                .packageQuantity(firstVolume != null ? firstVolume.getQuantity() : null)
                .packageSpecies(firstVolume != null ? firstVolume.getSpecies() : null)
                .packageBrand(firstVolume != null ? firstVolume.getBrand() : null)
                .packageNumbering(firstVolume != null ? firstVolume.getNumbering() : null)
                .grossWeightKg(firstVolume != null ? firstVolume.getGrossWeightKg() : null)
                .netWeightKg(firstVolume != null ? firstVolume.getNetWeightKg() : null)
                .freightAmount(freightAmount)
                .build();
    }

    private static String resolvePaymentMethod(InvoiceBillingData billing) {
        if (billing == null || billing.getPayments() == null || billing.getPayments().isEmpty()) {
            return null;
        }
        return billing.getPayments().get(0).getPaymentMethod() != null
                ? billing.getPayments().get(0).getPaymentMethod().name()
                : null;
    }

    private static LocalDate resolveFirstDueDate(InvoiceBillingData billing) {
        if (billing == null || billing.getInstallments() == null || billing.getInstallments().isEmpty()) {
            return null;
        }
        return billing.getInstallments().get(0).getDueDate();
    }
}
