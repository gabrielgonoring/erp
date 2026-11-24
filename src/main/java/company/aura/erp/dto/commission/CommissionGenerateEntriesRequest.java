package company.aura.erp.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionGenerateEntriesRequest {
    private Long collaboratorId;
    private Long orderId;
    private Long serviceOrderId;
    private LocalDate startDate;
    private LocalDate endDate;
}
