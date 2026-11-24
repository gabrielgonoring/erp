package company.aura.erp.dto.order;

import com.fasterxml.jackson.annotation.JsonAlias;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.PaymentTransactionType;
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
public class PaymentTransactionRequest {
    @NotNull(message = "O valor pago é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor pago deve ser maior que zero.")
    @JsonAlias({"amount", "amountPaid"})
    private BigDecimal amountPaid;

    @NotNull(message = "A forma de pagamento é obrigatória.")
    private PaymentMethod paymentMethod;

    private LocalDateTime paidAt;

    @Size(max = 50, message = "A bandeira do cartão deve ter no máximo 50 caracteres.")
    private String cardBrand;

    @Size(max = 50, message = "A autorização/NSU deve ter no máximo 50 caracteres.")
    private String nsuAuthorization;

    @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
    private String notes;

    private PaymentTransactionType transactionType;

    private Long refundedTransactionId;

    public PaymentTransactionRequest(
            BigDecimal amountPaid,
            PaymentMethod paymentMethod,
            LocalDateTime paidAt,
            String cardBrand,
            String nsuAuthorization,
            String notes
    ) {
        this(amountPaid, paymentMethod, paidAt, cardBrand, nsuAuthorization, notes, PaymentTransactionType.PAYMENT, null);
    }
}
