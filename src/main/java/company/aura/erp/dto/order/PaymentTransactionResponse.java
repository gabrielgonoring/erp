package company.aura.erp.dto.order;

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
public class PaymentTransactionResponse {
    private Long id;
    private Long installmentId;
    private Long companyId;
    private LocalDateTime paidAt;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private String cardBrand;
    private String nsuAuthorization;
    private String notes;
    private LocalDateTime createdAt;
}
