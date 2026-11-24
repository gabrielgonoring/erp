package company.aura.erp.dto.commission;

import company.aura.erp.enums.CommissionEntryStatus;
import company.aura.erp.enums.CommissionRuleSource;
import company.aura.erp.enums.CommissionSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionEntryResponse {
    private Long id;
    private Long companyId;
    private Long collaboratorId;
    private String collaboratorName;
    private Long statementId;
    private CommissionSourceType sourceType;
    private Long sourceDocumentId;
    private Long sourceItemId;
    private Long catalogItemId;
    private Long categoryId;
    private String itemDescription;
    private BigDecimal baseAmount;
    private BigDecimal appliedPercentage;
    private BigDecimal commissionAmount;
    private Long ruleId;
    private CommissionRuleSource ruleSource;
    private CommissionEntryStatus status;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
