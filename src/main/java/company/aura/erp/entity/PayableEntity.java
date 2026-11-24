package company.aura.erp.entity;

import company.aura.erp.enums.PayableOriginType;
import company.aura.erp.enums.PayableStatus;
import company.aura.erp.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PayableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "collaborator_id")
    private Long collaboratorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 45)
    private PayableOriginType originType;

    @Column(name = "origin_id")
    private Long originId;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

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
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayableStatus status = PayableStatus.PENDING;

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
            this.originType = PayableOriginType.MANUAL;
        }
        if (this.installmentNumber == null) {
            this.installmentNumber = 1;
        }
        if (this.status == null) {
            this.status = PayableStatus.PENDING;
        }
        if (this.pendingAmount == null) {
            if (this.status == PayableStatus.PAID || this.status == PayableStatus.CANCELED) {
                this.pendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else if (this.amount != null) {
                this.pendingAmount = this.amount.setScale(2, RoundingMode.HALF_UP);
            } else {
                this.pendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }
    }
}
