package company.aura.erp.mapper;

import company.aura.erp.dto.order.PaymentTransactionResponse;
import company.aura.erp.dto.order.ReceivableResponse;
import company.aura.erp.dto.order.ReceivableUpdateRequest;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.entity.ReceivableEntity;
import company.aura.erp.enums.ReceivableStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class ReceivableMapper {

    public static void updateEntity(ReceivableEntity entity, ReceivableUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartnerId(request.getPartnerId());
        entity.setInstallmentNumber(request.getInstallmentNumber());
        entity.setDueDate(request.getDueDate());
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setBankSlipCode(request.getBankSlipCode());
        entity.setPaymentFileUrl(request.getPaymentFileUrl());
        entity.setPaymentUrl(request.getPaymentUrl());
        entity.setNotes(request.getNotes());
    }

    public static ReceivableResponse fromEntity(ReceivableEntity entity) {
        if (entity == null) {
            return null;
        }
        return fromEntity(entity, Collections.emptyList());
    }

    public static ReceivableResponse fromEntity(
            ReceivableEntity entity,
            List<PaymentTransactionEntity> transactions
    ) {
        if (entity == null) {
            return null;
        }

        List<PaymentTransactionResponse> txResponses = transactions != null
                ? transactions.stream().map(PaymentTransactionMapper::fromEntity).toList()
                : Collections.emptyList();

        BigDecimal totalAmount = entity.getAmount() != null ? entity.getAmount() : BigDecimal.ZERO;

        BigDecimal paid;

        if (entity.getStatus() == ReceivableStatus.PAID) {
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

        return ReceivableResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .serviceOrderId(entity.getServiceOrderId())
                .originType(entity.getOriginType())
                .companyId(entity.getCompanyId())
                .partnerId(entity.getPartnerId())
                .installmentNumber(entity.getInstallmentNumber())
                .dueDate(entity.getDueDate())
                .amount(entity.getAmount())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .bankSlipCode(entity.getBankSlipCode())
                .paymentFileUrl(entity.getPaymentFileUrl())
                .paymentUrl(entity.getPaymentUrl())
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
