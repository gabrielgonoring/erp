package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record PendingReceivableSummary(
        Long count,
        BigDecimal totalAmount
) {}
