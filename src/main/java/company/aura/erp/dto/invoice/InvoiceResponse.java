package company.aura.erp.dto.invoice;

import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private Long companyId;
    private Long partnerId;
    private String externalReference;
    private Integer operationType;
    private DocumentType documentType;
    private EnvironmentType environment;
    private String status;
    private Integer invoiceNumber;
    private Integer series;
    private String accessKey;
    private String authorizationProtocol;
    private String paymentMethod;
    private LocalDate dueDate;
    private Integer freightMode;
    private BigDecimal freightAmount;
    private BigDecimal additionalExpenses;
    private BigDecimal productsAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String additionalInformation;
    private String pdfUrl;
    private String xmlUrl;
    private LocalDateTime issuedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
