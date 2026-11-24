package company.aura.erp.dto.payable;

import com.fasterxml.jackson.annotation.JsonAlias;
import company.aura.erp.enums.PayableTransactionType;
import company.aura.erp.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PayableTransactionRequest {

    @NotNull(message = "O valor pago é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor pago deve ser maior que zero.")
    @JsonAlias({"amount", "amountPaid"})
    private BigDecimal amountPaid;

    @NotNull(message = "A forma de pagamento é obrigatória.")
    private PaymentMethod paymentMethod;

    private LocalDateTime paidAt;

    private String receiptUrl;

    @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
    private String notes;

    private PayableTransactionType transactionType;

    public PayableTransactionRequest(
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            LocalDateTime paidAt,
            String receiptUrl,
            String notes
    ) {
        this(amountPaid, paymentMethod, paidAt, receiptUrl, notes, PayableTransactionType.PAYMENT);
    }
}
