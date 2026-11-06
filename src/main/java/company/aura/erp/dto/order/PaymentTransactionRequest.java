package company.aura.erp.dto.order;

import com.fasterxml.jackson.annotation.JsonAlias;
import company.aura.erp.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionRequest(
        @NotNull(message = "O valor pago é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor pago deve ser maior que zero.")
        @JsonAlias({"amount", "amountPaid"})
        BigDecimal amountPaid,

        @NotNull(message = "A forma de pagamento é obrigatória.")
        PaymentMethod paymentMethod,

        LocalDateTime paidAt,

        @Size(max = 50, message = "A bandeira do cartão deve ter no máximo 50 caracteres.")
        String cardBrand,

        @Size(max = 50, message = "A autorização/NSU deve ter no máximo 50 caracteres.")
        String nsuAuthorization,

        @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
        String notes
) {}
