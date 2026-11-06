package company.aura.erp.mapper;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import company.aura.erp.security.UserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceMapper {

    public static InvoiceEntity buildInvoiceEntity(UserPrincipal currentUser, InvoiceCreateRequest request, String xmlUrl, String pdfUrl){
        InvoiceEntity invoice = InvoiceEntity.builder()
                .companyId(currentUser.getCompanyId())
                .partnerId(request.partnerId())
                .externalReference(request.externalReference())
                .operationType(request.operationType() != null ? request.operationType() : 1)
                .documentType(request.documentType() != null ? request.documentType() : DocumentType.NFE)
                .environment(request.environment() != null ? request.environment() : EnvironmentType.STG)
                .status(request.status() != null && !request.status().isBlank() ? request.status() : "ISSUED")
                .invoiceNumber(request.invoiceNumber())
                .series(request.series())
                .accessKey(request.accessKey() != null ? request.accessKey().trim() : null)
                .authorizationProtocol(request.authorizationProtocol())
                .paymentMethod(request.paymentMethod())
                .dueDate(request.dueDate())
                .freightMode(request.freightMode() != null ? request.freightMode() : 9)
                .freightAmount(request.freightAmount() != null ? request.freightAmount() : BigDecimal.ZERO)
                .additionalExpenses(request.additionalExpenses() != null ? request.additionalExpenses() : BigDecimal.ZERO)
                .productsAmount(request.productsAmount() != null ? request.productsAmount() : BigDecimal.ZERO)
                .discountAmount(request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO)
                .totalAmount(request.totalAmount())
                .additionalInformation(request.additionalInformation())
                .issuedAt(request.issuedAt() != null ? request.issuedAt() : LocalDateTime.now())
                .xmlUrl(xmlUrl)
                .pdfUrl(pdfUrl)
                .build();

        return invoice;
    }
}
