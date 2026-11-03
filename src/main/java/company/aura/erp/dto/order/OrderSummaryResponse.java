package company.aura.erp.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        String customerDocument,
        String customerName,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {}
