package company.aura.erp.entity;

import company.aura.erp.enums.InvoicePurpose;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_order_invoice_purpose", columnNames = {"order_id", "invoice_id", "invoice_purpose"})
        },
        indexes = {
                @Index(name = "idx_order_invoices_order", columnList = "company_id, order_id"),
                @Index(name = "idx_order_invoices_invoice", columnList = "company_id, invoice_id"),
                @Index(name = "idx_order_invoices_purpose", columnList = "invoice_purpose")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class OrderInvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "invoice_purpose", nullable = false, length = 45)
    private InvoicePurpose invoicePurpose = InvoicePurpose.PRODUCT;

    @Column(name = "notes", length = 255)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
