package company.aura.erp.dto.dashboard;

import company.aura.erp.enums.OrderStatus;
import java.math.BigDecimal;

public record OrderStatusSummary(
        OrderStatus status,
        Long count
) {}