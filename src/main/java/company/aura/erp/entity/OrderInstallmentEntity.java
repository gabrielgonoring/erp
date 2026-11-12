package company.aura.erp.entity;

import company.aura.erp.enums.InstallmentStatus;
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
@Table(name = "order_installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class OrderInstallmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

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
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @Column(name = "bank_slip_code", length = 100)
    private String bankSlipCode;

    @Column(name = "bank_slip_url", columnDefinition = "TEXT")
    private String bankSlipUrl;

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
        if (this.pendingAmount == null) {
            if (this.status == InstallmentStatus.PAID || this.status == InstallmentStatus.CANCELED) {
                this.pendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else if (this.amount != null) {
                this.pendingAmount = this.amount.setScale(2, RoundingMode.HALF_UP);
            } else {
                this.pendingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }
    }
}
