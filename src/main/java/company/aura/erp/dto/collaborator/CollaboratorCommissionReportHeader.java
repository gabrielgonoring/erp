package company.aura.erp.dto.collaborator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorCommissionReportHeader {

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalSold;

    private BigDecimal totalCommission;

    private BigDecimal commissionPercentage;

}
