package company.aura.erp.dto.order;

import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableSummaryResponse(
        Long id,
        Long orderId,
        Long serviceOrderId,
        String customerDocument,
        String customerName,
        BigDecimal amount,
        BigDecimal pendingAmount,
        ReceivableStatus status,
        PaymentMethod paymentMethod,
        LocalDate dueDate
) {}
