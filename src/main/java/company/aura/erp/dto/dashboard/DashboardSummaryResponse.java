package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<OrderStatusSummary> salesPerStatus,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        BigDecimal totalAmount,
        PendingInstallmentSummary pendingInstallments,
        PaymentTransactionSummary revenue
) {
}
