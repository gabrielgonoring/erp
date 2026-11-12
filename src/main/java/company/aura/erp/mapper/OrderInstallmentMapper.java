package company.aura.erp.mapper;

import company.aura.erp.dto.order.OrderInstallmentResponse;
import company.aura.erp.dto.order.PaymentTransactionResponse;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class OrderInstallmentMapper {

    public static OrderInstallmentResponse fromEntity(OrderInstallmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return fromEntity(entity, Collections.emptyList());
    }

    public static OrderInstallmentResponse fromEntity(
            OrderInstallmentEntity entity,
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

        if(entity.getStatus() == InstallmentStatus.PAID) {
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

        return OrderInstallmentResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .companyId(entity.getCompanyId())
                .partnerId(entity.getPartnerId())
                .installmentNumber(entity.getInstallmentNumber())
                .dueDate(entity.getDueDate())
                .amount(entity.getAmount())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .bankSlipCode(entity.getBankSlipCode())
                .bankSlipUrl(entity.getBankSlipUrl())
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
