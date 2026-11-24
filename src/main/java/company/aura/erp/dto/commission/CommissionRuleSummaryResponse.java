package company.aura.erp.dto.commission;

import company.aura.erp.enums.CommissionRuleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRuleSummaryResponse {
    private Long id;
    private Long collaboratorId;
    private String collaboratorName;
    private Long categoryId;
    private String categoryName;
    private Long catalogItemId;
    private String catalogItemDescription;
    private BigDecimal commissionPercentage;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private CommissionRuleStatus status;
}
