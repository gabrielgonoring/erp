package company.aura.erp.dto.dashboard;

import company.aura.erp.enums.ServiceOrderStatus;

public record ServiceOrderStatusSummary(
        ServiceOrderStatus status,
        Long count
) {}
