package company.aura.erp.entity;

import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableOriginType;
import company.aura.erp.enums.ReceivableStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "receivables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ReceivableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "service_order_id")
    private Long serviceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 45)
    private ReceivableOriginType originType;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Builder.Default
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber = 1;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "pending_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceivableStatus status = ReceivableStatus.PENDING;

    @Column(name = "bank_slip_code", length = 100)
    private String bankSlipCode;

    @Column(name = "payment_file_url", columnDefinition = "TEXT")
    private String paymentFileUrl;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(name = "notes", length = 255)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.originType == null) {
            if (this.serviceOrderId != null) {
                this.originType = ReceivableOriginType.SERVICE_ORDER;
            } else if (this.orderId != null) {
                this.originType = ReceivableOriginType.ORDER;
            } else {
                this.originType = ReceivableOriginType.MANUAL;
            }
        }
        if (this.pendingAmount == null) {
            if (this.status == ReceivableStatus.PAID || this.status == ReceivableStatus.CANCELED) {
                this.pendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else if (this.amount != null) {
                this.pendingAmount = this.amount.setScale(2, RoundingMode.HALF_UP);
            } else {
                this.pendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }
    }
}
