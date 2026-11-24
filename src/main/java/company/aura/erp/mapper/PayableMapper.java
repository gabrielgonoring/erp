package company.aura.erp.mapper;

import company.aura.erp.dto.payable.PayableRequest;
import company.aura.erp.dto.payable.PayableResponse;
import company.aura.erp.dto.payable.PayableTransactionResponse;
import company.aura.erp.entity.PayableEntity;
import company.aura.erp.entity.PayableTransactionEntity;
import company.aura.erp.enums.PayableOriginType;
import company.aura.erp.enums.PayableStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PayableMapper {

    public static PayableEntity toEntity(Long companyId, PayableRequest request) {
        boolean isPaid = Boolean.TRUE.equals(request.getPaid());
        BigDecimal amount = request.getAmount() != null
                ? request.getAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pendingAmount = isPaid
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : amount;

        return PayableEntity.builder()
                .companyId(companyId)
                .partnerId(request.getPartnerId())
                .collaboratorId(request.getCollaboratorId())
                .originType(request.getOriginType() != null ? request.getOriginType() : PayableOriginType.MANUAL)
                .originId(request.getOriginId())
                .description(request.getDescription().trim())
                .installmentNumber(request.getInstallmentNumber() != null ? request.getInstallmentNumber() : 1)
                .dueDate(request.getDueDate())
                .amount(amount)
                .pendingAmount(pendingAmount)
                .paymentMethod(request.getPaymentMethod())
                .status(isPaid ? PayableStatus.PAID : PayableStatus.PENDING)
                .notes(request.getNotes())
                .build();
    }

    public static PayableResponse fromEntity(PayableEntity entity) {
        if (entity == null) {
            return null;
        }
        return fromEntity(entity, Collections.emptyList());
    }

    public static PayableResponse fromEntity(
            PayableEntity entity,
            List<PayableTransactionEntity> existingTransactions,
            PayableTransactionEntity newTransaction
    ) {
        List<PayableTransactionEntity> allTransactions = new ArrayList<>(
                existingTransactions != null ? existingTransactions : Collections.emptyList()
        );
        if (newTransaction != null) {
            allTransactions.add(newTransaction);
        }
        return fromEntity(entity, allTransactions);
    }

    public static PayableResponse fromEntity(
            PayableEntity entity,
            List<PayableTransactionEntity> transactions
    ) {
        if (entity == null) {
            return null;
        }

        List<PayableTransactionResponse> txResponses = transactions != null
                ? transactions.stream().map(PayableTransactionMapper::fromEntity).toList()
                : Collections.emptyList();

        BigDecimal totalAmount = entity.getAmount() != null ? entity.getAmount() : BigDecimal.ZERO;

        BigDecimal paid;
        if (entity.getStatus() == PayableStatus.PAID) {
            paid = totalAmount;
        } else if (entity.getPendingAmount() != null) {
            paid = totalAmount.subtract(entity.getPendingAmount());
            if (paid.compareTo(BigDecimal.ZERO) < 0) {
                paid = BigDecimal.ZERO;
            }
        } else {
            paid = BigDecimal.ZERO;
        }

        BigDecimal remaining;
        if (entity.getPendingAmount() != null) {
            remaining = entity.getPendingAmount();
        } else {
            remaining = totalAmount.subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                remaining = BigDecimal.ZERO;
            }
        }

        return PayableResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .partnerId(entity.getPartnerId())
                .collaboratorId(entity.getCollaboratorId())
                .originType(entity.getOriginType())
                .originId(entity.getOriginId())
                .description(entity.getDescription())
                .installmentNumber(entity.getInstallmentNumber())
                .dueDate(entity.getDueDate())
                .amount(entity.getAmount())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .paidAmount(paid)
                .remainingAmount(remaining)
                .pendingAmount(remaining)
                .transactions(txResponses)
                .build();
    }
}
