package company.aura.erp.mapper;

import company.aura.erp.dto.payable.PayableRequest;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.dto.payable.PayableTransactionResponse;
import company.aura.erp.entity.PayableEntity;
import company.aura.erp.entity.PayableTransactionEntity;
import company.aura.erp.enums.PayableTransactionType;

import java.time.LocalDateTime;

public class PayableTransactionMapper {

    public static PayableTransactionEntity toEntity(
            Long companyId,
            PayableEntity payable,
            PayableRequest request
    ) {
        return PayableTransactionEntity.builder()
                .payableId(payable.getId())
                .companyId(companyId)
                .transactionType(PayableTransactionType.PAYMENT)
                .paidAt(LocalDateTime.now())
                .amountPaid(payable.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .receiptUrl(request.getReceiptUrl())
                .notes(request.getNotes())
                .build();
    }

    public static PayableTransactionEntity toEntity(
            Long companyId,
            Long payableId,
            PayableTransactionType transactionType,
            PayableTransactionRequest request
    ) {
        LocalDateTime paidAt = request.getPaidAt() != null ? request.getPaidAt() : LocalDateTime.now();
        return PayableTransactionEntity.builder()
                .payableId(payableId)
                .companyId(companyId)
                .transactionType(transactionType)
                .paidAt(paidAt)
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .receiptUrl(request.getReceiptUrl())
                .notes(request.getNotes())
                .build();
    }

    public static PayableTransactionResponse fromEntity(PayableTransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        return PayableTransactionResponse.builder()
                .id(entity.getId())
                .payableId(entity.getPayableId())
                .companyId(entity.getCompanyId())
                .transactionType(entity.getTransactionType())
                .paidAt(entity.getPaidAt())
                .amountPaid(entity.getAmountPaid())
                .paymentMethod(entity.getPaymentMethod())
                .receiptUrl(entity.getReceiptUrl())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
