package company.aura.erp.dto.order;

import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        Long id,
        Long installmentId,
        Long companyId,
        LocalDateTime paidAt,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        String cardBrand,
        String nsuAuthorization,
        String notes,
        LocalDateTime createdAt
) {
    public static PaymentTransactionResponse fromEntity(PaymentTransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentTransactionResponse(
                entity.getId(),
                entity.getInstallmentId(),
                entity.getCompanyId(),
                entity.getPaidAt(),
                entity.getAmountPaid(),
                entity.getPaymentMethod(),
                entity.getCardBrand(),
                entity.getNsuAuthorization(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}
