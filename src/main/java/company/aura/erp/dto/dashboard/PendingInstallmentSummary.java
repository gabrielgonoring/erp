package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record PendingInstallmentSummary(
        Long count,
        BigDecimal totalAmount
) {}
