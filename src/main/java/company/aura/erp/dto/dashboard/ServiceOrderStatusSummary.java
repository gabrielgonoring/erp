package company.aura.erp.dto.dashboard;

import company.aura.erp.enums.ServiceOrderStatus;

import java.math.BigDecimal;

public record ServiceOrderStatusSummary(
        ServiceOrderStatus status,
        Long count,
        BigDecimal totalAmount
) {
    public ServiceOrderStatusSummary {
        count = count != null ? count : 0L;
        totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }
}
