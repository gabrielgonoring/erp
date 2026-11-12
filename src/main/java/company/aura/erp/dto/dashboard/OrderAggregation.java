package company.aura.erp.dto.dashboard;

import java.math.BigDecimal;

public record OrderAggregation(
        Long count,
        BigDecimal totalAmount
) {}
