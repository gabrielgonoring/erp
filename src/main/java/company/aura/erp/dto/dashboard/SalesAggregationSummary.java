package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record SalesAggregationSummary(
        BigDecimal totalAmount,
        Long count
) {
    public SalesAggregationSummary {
        totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        count = count != null ? count : 0L;
    }
}
