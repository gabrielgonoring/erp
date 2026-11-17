package company.aura.erp.dto.order;

import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderInstallmentSummaryResponse(
        Long id,
        Long orderId,
        String customerDocument,
        String customerName,
        BigDecimal amount,
        BigDecimal pendingAmount,
        InstallmentStatus status,
        PaymentMethod paymentMethod,
        LocalDate dueDate
) {}
