package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record InstallmentPaymentSummary(
        BigDecimal paidAmount,
        BigDecimal pendingAmount
) {}