package company.aura.erp.dto.commission;

import company.aura.erp.enums.CommissionStatementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionStatementResponse {
    private Long id;
    private Long companyId;
    private Long collaboratorId;
    private String collaboratorName;
    private Long payableId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate dueDate;
    private BigDecimal totalSalesAmount;
    private BigDecimal totalCommissionAmount;
    private CommissionStatementStatus status;
    private LocalDateTime paidAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommissionEntryResponse> entries;
}
