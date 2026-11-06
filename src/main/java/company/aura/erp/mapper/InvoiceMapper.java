package company.aura.erp.mapper;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import company.aura.erp.security.UserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceMapper {

    public static InvoiceEntity buildInvoiceEntity(UserPrincipal currentUser, InvoiceCreateRequest request, String xmlUrl, String pdfUrl) {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .companyId(currentUser.getCompanyId())
                .partnerId(request.getPartnerId())
                .externalReference(request.getExternalReference())
                .operationType(request.getOperationType() != null ? request.getOperationType() : 1)
                .documentType(request.getDocumentType() != null ? request.getDocumentType() : DocumentType.NFE)
                .environment(request.getEnvironment() != null ? request.getEnvironment() : EnvironmentType.STG)
                .status(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus() : "ISSUED")
                .invoiceNumber(request.getInvoiceNumber())
                .series(request.getSeries())
                .accessKey(request.getAccessKey() != null ? request.getAccessKey().trim() : null)
                .authorizationProtocol(request.getAuthorizationProtocol())
                .paymentMethod(request.getPaymentMethod())
                .dueDate(request.getDueDate())
                .freightMode(request.getFreightMode() != null ? request.getFreightMode() : 9)
                .freightAmount(request.getFreightAmount() != null ? request.getFreightAmount() : BigDecimal.ZERO)
                .additionalExpenses(request.getAdditionalExpenses() != null ? request.getAdditionalExpenses() : BigDecimal.ZERO)
                .productsAmount(request.getProductsAmount() != null ? request.getProductsAmount() : BigDecimal.ZERO)
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .totalAmount(request.getTotalAmount())
                .additionalInformation(request.getAdditionalInformation())
                .issuedAt(request.getIssuedAt() != null ? request.getIssuedAt() : LocalDateTime.now())
                .xmlUrl(xmlUrl)
                .pdfUrl(pdfUrl)
                .build();

        return invoice;
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
}
