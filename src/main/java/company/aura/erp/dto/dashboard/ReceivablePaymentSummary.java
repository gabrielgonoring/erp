package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record ReceivablePaymentSummary(
        BigDecimal paidAmount,
        BigDecimal pendingAmount
) {}
