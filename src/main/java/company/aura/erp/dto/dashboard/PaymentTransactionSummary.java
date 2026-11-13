package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record PaymentTransactionSummary(
        Long count,
        BigDecimal totalAmount
) {
    public PaymentTransactionSummary {
        count = count != null ? count : 0L;
        totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }
}
