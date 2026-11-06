package company.aura.erp.mapper;

import company.aura.erp.dto.order.PaymentTransactionResponse;
import company.aura.erp.entity.PaymentTransactionEntity;

public class PaymentTransactionMapper {

    public static PaymentTransactionResponse fromEntity(PaymentTransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        return PaymentTransactionResponse.builder()
                .id(entity.getId())
                .installmentId(entity.getInstallmentId())
                .companyId(entity.getCompanyId())
                .paidAt(entity.getPaidAt())
                .amountPaid(entity.getAmountPaid())
                .paymentMethod(entity.getPaymentMethod())
                .cardBrand(entity.getCardBrand())
                .nsuAuthorization(entity.getNsuAuthorization())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
