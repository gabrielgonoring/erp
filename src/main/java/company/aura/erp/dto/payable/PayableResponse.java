package company.aura.erp.dto.payable;

import company.aura.erp.enums.PayableOriginType;
import company.aura.erp.enums.PayableStatus;
import company.aura.erp.enums.PaymentMethod;
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
public class PayableResponse {
    private Long id;
    private Long companyId;
    private Long partnerId;
    private Long collaboratorId;
    private PayableOriginType originType;
    private Long originId;
    private String description;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PayableStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal pendingAmount;
    private List<PayableTransactionResponse> transactions;
}
