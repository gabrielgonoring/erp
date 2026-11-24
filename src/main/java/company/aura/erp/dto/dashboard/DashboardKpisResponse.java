package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardKpisResponse(
        Period period,
        Kpis kpis
) {
    public record Period(
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record Kpis(
            RealizedRevenue realizedRevenue,
            GeneratedSales generatedSales,
            OpenReceivables openReceivables,
            OverdueReceivables overdueReceivables
    ) {}

    public record RealizedRevenue(
            BigDecimal amount,
            BigDecimal previousAmount,
            BigDecimal growthPercentage,
            Long paymentsCount
    ) {}

    public record GeneratedSales(
            BigDecimal totalAmount,
            BigDecimal ordersAmount,
            Long ordersCount,
            BigDecimal serviceOrdersAmount,
            Long serviceOrdersCount
    ) {}

    public record OpenReceivables(
            BigDecimal totalAmount,
            Long installmentsCount,
            BigDecimal dueThisWeekAmount,
            Long dueThisWeekCount
    ) {}

    public record OverdueReceivables(
            BigDecimal totalAmount,
            Long installmentsCount,
            Long customersCount
    ) {}
}
