package company.aura.erp.dto.dashboard;

import company.aura.erp.enums.OrderStatus;

import java.math.BigDecimal;

public record OrderStatusTotalSummary(
        OrderStatus status,
        Long count,
        BigDecimal totalAmount
) {
    public OrderStatusTotalSummary {
        count = count != null ? count : 0L;
        totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }
}
