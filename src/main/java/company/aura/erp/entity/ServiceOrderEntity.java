package company.aura.erp.entity;

import company.aura.erp.enums.ServiceOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ServiceOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "order_id", unique = true)
    private Long orderId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ServiceOrderStatus status = ServiceOrderStatus.TRIAGE;

    @Column(name = "object_title", nullable = false, length = 150)
    private String objectTitle;

    @Column(name = "object_identifier", length = 100)
    private String objectIdentifier;

    @Column(name = "entry_details", columnDefinition = "TEXT")
    private String entryDetails;

    @Column(name = "reported_issue", nullable = false, columnDefinition = "TEXT")
    private String reportedIssue;

    @Column(name = "technical_report", columnDefinition = "TEXT")
    private String technicalReport;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Builder.Default
    @Column(name = "services_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal servicesAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "products_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal productsAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "entry_at", nullable = false)
    private LocalDateTime entryAt;

    @Column(name = "expected_delivery_at")
    private LocalDateTime expectedDeliveryAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = ServiceOrderStatus.TRIAGE;
        }
        if (this.entryAt == null) {
            this.entryAt = LocalDateTime.now();
        }
        if (this.servicesAmount == null) {
            this.servicesAmount = BigDecimal.ZERO;
        }
        if (this.productsAmount == null) {
            this.productsAmount = BigDecimal.ZERO;
        }
        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }
}
