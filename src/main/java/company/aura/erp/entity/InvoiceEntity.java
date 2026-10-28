package company.aura.erp.entity;

import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "external_reference", length = 100)
    private String externalReference;
    
    @Column(name = "operation_type", nullable = false)
    private Integer operationType = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 10)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 10)
    private EnvironmentType environment;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "invoice_number")
    private Integer invoiceNumber;

    @Column(name = "series")
    private Integer series;

    @Column(name = "access_key", length = 44)
    private String accessKey;

    @Column(name = "authorization_protocol", length = 100)
    private String authorizationProtocol;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "freight_mode")
    private Integer freightMode = 9;

    @Column(name = "freight_amount", precision = 12, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;
    
    @Column(name = "additional_expenses", precision = 12, scale = 2)
    private BigDecimal additionalExpenses = BigDecimal.ZERO;
    
    @Column(name = "products_amount", precision = 12, scale = 2)
    private BigDecimal productsAmount = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "additional_information", columnDefinition = "TEXT")
    private String additionalInformation;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancellation_protocol", length = 100)
    private String cancellationProtocol;

    @Column(name = "correction_letter", columnDefinition = "TEXT")
    private String correctionLetter;

    @Column(name = "notaas_id", length = 100)
    private String notaasId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "xml_url", columnDefinition = "TEXT")
    private String xmlUrl;

    @Column(name = "raw_payload_sent", columnDefinition = "json")
    private String rawPayloadSent;

    @Column(name = "raw_response_received", columnDefinition = "json")
    private String rawResponseReceived;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
