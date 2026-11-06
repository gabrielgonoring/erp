package company.aura.erp.dto.order;

import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record OrderInstallmentResponse(
        Long id,
        Long orderId,
        Long companyId,
        Long partnerId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        InstallmentStatus status,
        String bankSlipCode,
        String bankSlipUrl,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        List<PaymentTransactionResponse> transactions
) {
    public OrderInstallmentResponse(
            Long id,
            Long orderId,
            Long companyId,
            Long partnerId,
            Integer installmentNumber,
            LocalDate dueDate,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            InstallmentStatus status,
            String bankSlipCode,
            String bankSlipUrl,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(
                id,
                orderId,
                companyId,
                partnerId,
                installmentNumber,
                dueDate,
                amount,
                paymentMethod,
                status,
                bankSlipCode,
                bankSlipUrl,
                notes,
                createdAt,
                updatedAt,
                status == InstallmentStatus.PAID ? amount : BigDecimal.ZERO,
                status == InstallmentStatus.PAID ? BigDecimal.ZERO : amount,
                Collections.emptyList()
        );
    }

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
                ? transactions.stream().map(PaymentTransactionResponse::fromEntity).toList()
                : Collections.emptyList();

        BigDecimal paid;
        if (transactions != null && !transactions.isEmpty()) {
            paid = transactions.stream()
                    .map(PaymentTransactionEntity::getAmountPaid)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else if (entity.getStatus() == InstallmentStatus.PAID) {
            paid = entity.getAmount() != null ? entity.getAmount() : BigDecimal.ZERO;
        } else {
            paid = BigDecimal.ZERO;
        }

        BigDecimal totalAmount = entity.getAmount() != null ? entity.getAmount() : BigDecimal.ZERO;
        BigDecimal remaining = totalAmount.subtract(paid);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            remaining = BigDecimal.ZERO;
        }

        return new OrderInstallmentResponse(
                entity.getId(),
                entity.getOrderId(),
                entity.getCompanyId(),
                entity.getPartnerId(),
                entity.getInstallmentNumber(),
                entity.getDueDate(),
                entity.getAmount(),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getBankSlipCode(),
                entity.getBankSlipUrl(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                paid,
                remaining,
                txResponses
        );
    }
}
