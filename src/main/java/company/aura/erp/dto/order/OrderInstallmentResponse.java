package company.aura.erp.dto.order;

import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInstallmentResponse {
    private Long id;
    private Long orderId;
    private Long companyId;
    private Long partnerId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private InstallmentStatus status;
    private String bankSlipCode;
    private String bankSlipUrl;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private List<PaymentTransactionResponse> transactions;

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
}
