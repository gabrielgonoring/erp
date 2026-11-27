package company.aura.erp.mapper;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import company.aura.erp.enums.InvoiceStatus;
import company.aura.erp.modules.invoice.domain.InvoiceAdditionalInfoData;
import company.aura.erp.modules.invoice.domain.InvoiceBillingData;
import company.aura.erp.modules.invoice.domain.InvoiceFreightData;
import company.aura.erp.modules.invoice.domain.InvoiceIdentificationData;
import company.aura.erp.modules.invoice.domain.InvoiceProtocolData;
import company.aura.erp.modules.invoice.domain.InvoiceTotalsData;
import company.aura.erp.modules.invoice.domain.ParsedInvoiceXml;
import company.aura.erp.security.UserPrincipal;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InvoiceMapper {

    private static final int DEFAULT_OPERATION_TYPE = 1;
    private static final int DEFAULT_FREIGHT_MODE = 9;

    public static InvoiceEntity buildInvoiceEntity(
            UserPrincipal currentUser,
            InvoiceCreateRequest request,
            ParsedInvoiceXml parsedXml,
            String xmlUrl,
            String pdfUrl
    ) {
        if (parsedXml == null) {
            return null;
        }

        Long partnerId = request != null ? request.getPartnerId() : null;

        InvoiceEntity.InvoiceEntityBuilder builder = InvoiceEntity.builder()
                .companyId(currentUser.getCompanyId())
                .partnerId(partnerId)
                .status(InvoiceStatus.ISSUED.name())
                .xmlUrl(xmlUrl)
                .pdfUrl(pdfUrl);

        applyIdentification(builder, parsedXml.getIdentification());
        applyProtocol(builder, parsedXml.getProtocol());
        applyTotals(builder, parsedXml.getTotals());
        applyFreight(builder, parsedXml.getFreight());
        applyBilling(builder, parsedXml.getBilling());
        applyAdditionalInfo(builder, parsedXml.getAdditionalInfo());

        return builder.build();
    }

    public static InvoiceResponse fromEntity(InvoiceEntity entity) {
        if (entity == null) {
            return null;
        }
        return InvoiceResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .partnerId(entity.getPartnerId())
                .externalReference(entity.getExternalReference())
                .operationType(entity.getOperationType())
                .documentType(entity.getDocumentType())
                .environment(entity.getEnvironment())
                .status(entity.getStatus())
                .invoiceNumber(entity.getInvoiceNumber())
                .series(entity.getSeries())
                .accessKey(entity.getAccessKey())
                .authorizationProtocol(entity.getAuthorizationProtocol())
                .paymentMethod(entity.getPaymentMethod())
                .dueDate(entity.getDueDate())
                .freightMode(entity.getFreightMode())
                .freightAmount(entity.getFreightAmount())
                .additionalExpenses(entity.getAdditionalExpenses())
                .productsAmount(entity.getProductsAmount())
                .discountAmount(entity.getDiscountAmount())
                .totalAmount(entity.getTotalAmount())
                .additionalInformation(entity.getAdditionalInformation())
                .pdfUrl(entity.getPdfUrl())
                .xmlUrl(entity.getXmlUrl())
                .issuedAt(entity.getIssuedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static void applyIdentification(InvoiceEntity.InvoiceEntityBuilder builder, InvoiceIdentificationData id) {
        if (id == null) {
            builder.operationType(DEFAULT_OPERATION_TYPE)
                    .documentType(DocumentType.NFE)
                    .environment(EnvironmentType.PRD)
                    .issuedAt(LocalDateTime.now());
            return;
        }
        builder.operationType(id.getOperationType() != null ? id.getOperationType() : DEFAULT_OPERATION_TYPE)
                .documentType(id.getDocumentType() != null ? id.getDocumentType() : DocumentType.NFE)
                .environment(id.getEnvironment() != null ? id.getEnvironment() : EnvironmentType.PRD)
                .invoiceNumber(id.getInvoiceNumber())
                .series(id.getSeries())
                .accessKey(id.getAccessKey() != null ? id.getAccessKey().trim() : null)
                .issuedAt(id.getIssuedAt() != null ? id.getIssuedAt() : LocalDateTime.now());
    }

    private static void applyProtocol(InvoiceEntity.InvoiceEntityBuilder builder, InvoiceProtocolData protocol) {
        if (protocol == null) {
            return;
        }
        builder.authorizationProtocol(protocol.getAuthorizationProtocol());
    }

    private static void applyTotals(InvoiceEntity.InvoiceEntityBuilder builder, InvoiceTotalsData totals) {
        if (totals == null) {
            builder.freightAmount(BigDecimal.ZERO)
                    .additionalExpenses(BigDecimal.ZERO)
                    .productsAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO);
            return;
        }
        builder.freightAmount(defaultZero(totals.getFreightAmount()))
                .additionalExpenses(defaultZero(totals.getAdditionalExpenses()))
                .productsAmount(defaultZero(totals.getProductsAmount()))
                .discountAmount(defaultZero(totals.getDiscountAmount()))
                .totalAmount(defaultZero(totals.getTotalAmount()));
    }

    private static void applyFreight(InvoiceEntity.InvoiceEntityBuilder builder, InvoiceFreightData freight) {
        builder.freightMode(freight != null && freight.getFreightModeCode() != null
                ? freight.getFreightModeCode()
                : DEFAULT_FREIGHT_MODE);
    }

    private static void applyBilling(InvoiceEntity.InvoiceEntityBuilder builder, InvoiceBillingData billing) {
        builder.paymentMethod(resolvePaymentMethod(billing))
                .dueDate(resolveFirstDueDate(billing));
    }

    private static void applyAdditionalInfo(InvoiceEntity.InvoiceEntityBuilder builder, InvoiceAdditionalInfoData additionalInfo) {
        if (additionalInfo == null) {
            return;
        }
        String info = StringUtils.isNotBlank(additionalInfo.getTaxpayerInfo())
                ? additionalInfo.getTaxpayerInfo()
                : additionalInfo.getFiscoInfo();
        builder.additionalInformation(info);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
