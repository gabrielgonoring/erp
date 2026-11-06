package company.aura.erp.dto.invoice;

import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long id,
        Long companyId,
        Long partnerId,
        String externalReference,
        Integer operationType,
        DocumentType documentType,
        EnvironmentType environment,
        String status,
        Integer invoiceNumber,
        Integer series,
        String accessKey,
        String authorizationProtocol,
        String paymentMethod,
        LocalDate dueDate,
        Integer freightMode,
        BigDecimal freightAmount,
        BigDecimal additionalExpenses,
        BigDecimal productsAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String additionalInformation,
        String pdfUrl,
        String xmlUrl,
        LocalDateTime issuedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InvoiceResponse fromEntity(InvoiceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new InvoiceResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getPartnerId(),
                entity.getExternalReference(),
                entity.getOperationType(),
                entity.getDocumentType(),
                entity.getEnvironment(),
                entity.getStatus(),
                entity.getInvoiceNumber(),
                entity.getSeries(),
                entity.getAccessKey(),
                entity.getAuthorizationProtocol(),
                entity.getPaymentMethod(),
                entity.getDueDate(),
                entity.getFreightMode(),
                entity.getFreightAmount(),
                entity.getAdditionalExpenses(),
                entity.getProductsAmount(),
                entity.getDiscountAmount(),
                entity.getTotalAmount(),
                entity.getAdditionalInformation(),
                entity.getPdfUrl(),
                entity.getXmlUrl(),
                entity.getIssuedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
