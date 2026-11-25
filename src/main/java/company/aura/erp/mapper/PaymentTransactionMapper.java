package company.aura.erp.mapper;

import company.aura.erp.dto.order.PaymentTransactionResponse;
import company.aura.erp.dto.order.PaymentTransactionUpdateRequest;
import company.aura.erp.entity.PaymentTransactionEntity;

public class PaymentTransactionMapper {

    public static void updateEntity(PaymentTransactionEntity entity, PaymentTransactionUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPaymentMethod(request.getPaymentMethod());
        if (request.getPaidAt() != null) {
            entity.setPaidAt(request.getPaidAt());
        }
        entity.setCardBrand(request.getCardBrand());
        entity.setNsuAuthorization(request.getNsuAuthorization());
        entity.setNotes(request.getNotes());
    }

    public static PaymentTransactionResponse fromEntity(PaymentTransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        return PaymentTransactionResponse.builder()
                .id(entity.getId())
                .receivableId(entity.getReceivableId())
                .refundedTransactionId(entity.getRefundedTransactionId())
                .companyId(entity.getCompanyId())
                .transactionType(entity.getTransactionType())
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
