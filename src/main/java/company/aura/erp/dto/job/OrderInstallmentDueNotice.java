package company.aura.erp.dto.job;

import company.aura.erp.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderInstallmentDueNotice(
        Long installmentId,
        Long orderId,
        Integer installmentNumber,
        BigDecimal amount,
        BigDecimal pendingAmount,
        LocalDate dueDate,
        PaymentMethod paymentMethod,
        String bankSlipUrl,
        String bankSlipCode,
        Long companyId,
        String customerName,
        String customerEmail,
        String customerBillingEmail
) {
    public String resolveRecipientEmail() {
        if (customerBillingEmail != null && !customerBillingEmail.isBlank()) {
            return customerBillingEmail.trim();
        }
        if (customerEmail != null && !customerEmail.isBlank()) {
            return customerEmail.trim();
        }
        return null;
    }
}
