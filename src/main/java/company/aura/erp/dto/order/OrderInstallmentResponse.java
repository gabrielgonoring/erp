package company.aura.erp.dto.order;

import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
        LocalDateTime updatedAt
) {
    public static OrderInstallmentResponse fromEntity(OrderInstallmentEntity entity) {
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
                entity.getUpdatedAt()
        );
    }
}
