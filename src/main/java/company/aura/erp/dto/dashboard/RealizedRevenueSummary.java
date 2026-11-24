package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record RealizedRevenueSummary(
        BigDecimal currentRevenue,
        Long paymentsCount,
        BigDecimal previousRevenue
) {
    public RealizedRevenueSummary {
        currentRevenue = currentRevenue != null ? currentRevenue : BigDecimal.ZERO;
        paymentsCount = paymentsCount != null ? paymentsCount : 0L;
        previousRevenue = previousRevenue != null ? previousRevenue : BigDecimal.ZERO;
    }
}
