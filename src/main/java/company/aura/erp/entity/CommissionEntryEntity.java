package company.aura.erp.entity;

import company.aura.erp.enums.CommissionEntryStatus;
import company.aura.erp.enums.CommissionRuleSource;
import company.aura.erp.enums.CommissionSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CommissionEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "collaborator_id", nullable = false)
    private Long collaboratorId;

    @Column(name = "collaborator_name", nullable = false, length = 255)
    private String collaboratorName;

    @Column(name = "statement_id")
    private Long statementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private CommissionSourceType sourceType;

    @Column(name = "source_document_id", nullable = false)
    private Long sourceDocumentId;

    @Column(name = "source_item_id", nullable = false)
    private Long sourceItemId;

    @Column(name = "catalog_item_id")
    private Long catalogItemId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "item_description", nullable = false, length = 255)
    private String itemDescription;

    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "applied_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal appliedPercentage;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "rule_id")
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_source", nullable = false, length = 30)
    private CommissionRuleSource ruleSource;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommissionEntryStatus status = CommissionEntryStatus.PENDING;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = CommissionEntryStatus.PENDING;
        }
    }
}
