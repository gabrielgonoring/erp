package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalRevenue,
        SalesVolumeResponse salesVolume,
        PendingSalesResponse pendingSales
) {
    public record SalesVolumeResponse(
            Long closedOrdersCount,
            BigDecimal averageTicket
    ) {}

    public record PendingSalesResponse(
            BigDecimal totalAmount,
            Long ordersCount
    ) {}
}
