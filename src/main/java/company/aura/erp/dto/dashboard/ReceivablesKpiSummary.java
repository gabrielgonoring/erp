package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record ReceivablesKpiSummary(
        BigDecimal openAmount,
        Long openInstallmentsCount,
        BigDecimal dueThisWeekAmount,
        Long dueThisWeekCount,
        BigDecimal overdueAmount,
        Long overdueInstallmentsCount,
        Long overdueCustomersCount
) {
    public ReceivablesKpiSummary {
        openAmount = openAmount != null ? openAmount : BigDecimal.ZERO;
        openInstallmentsCount = openInstallmentsCount != null ? openInstallmentsCount : 0L;
        dueThisWeekAmount = dueThisWeekAmount != null ? dueThisWeekAmount : BigDecimal.ZERO;
        dueThisWeekCount = dueThisWeekCount != null ? dueThisWeekCount : 0L;
        overdueAmount = overdueAmount != null ? overdueAmount : BigDecimal.ZERO;
        overdueInstallmentsCount = overdueInstallmentsCount != null ? overdueInstallmentsCount : 0L;
        overdueCustomersCount = overdueCustomersCount != null ? overdueCustomersCount : 0L;
    }
}
