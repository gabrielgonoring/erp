package company.aura.erp.dto.order;

import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivableResponse {
    private Long id;
    private Long orderId;
    private Long serviceOrderId;
    private Long companyId;
    private Long partnerId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private ReceivableStatus status;
    private String bankSlipCode;
    private String paymentFileUrl;
    private String paymentUrl;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal pendingAmount;
    private List<PaymentTransactionResponse> transactions;

    public ReceivableResponse(
            Long id,
            Long orderId,
            Long companyId,
            Long partnerId,
            Integer installmentNumber,
            LocalDate dueDate,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            ReceivableStatus status,
            String bankSlipCode,
            String paymentFileUrl,
            String paymentUrl,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            List<PaymentTransactionResponse> transactions
    ) {
        this(
                id,
                orderId,
                null,
                companyId,
                partnerId,
                installmentNumber,
                dueDate,
                amount,
                paymentMethod,
                status,
                bankSlipCode,
                paymentFileUrl,
                paymentUrl,
                notes,
                createdAt,
                updatedAt,
                paidAmount,
                remainingAmount,
                remainingAmount,
                transactions
        );
    }
}
