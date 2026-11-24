package company.aura.erp.dto.payable;

import company.aura.erp.enums.PayableTransactionType;
import company.aura.erp.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayableTransactionResponse {
    private Long id;
    private Long payableId;
    private Long companyId;
    private PayableTransactionType transactionType;
    private LocalDateTime paidAt;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private String receiptUrl;
    private String notes;
    private LocalDateTime createdAt;
}
