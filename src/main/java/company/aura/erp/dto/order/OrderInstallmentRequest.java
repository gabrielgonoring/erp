package company.aura.erp.dto.order;

import company.aura.erp.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderInstallmentRequest(

        @NotNull(message = "O número da parcela precisa ser informada")
        Integer installmentNumber,

        @NotNull(message = "A data de vencimento da parcela é obrigatória.")
        LocalDate dueDate,

        @NotNull(message = "O valor da parcela é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor da parcela deve ser maior que zero.")
        BigDecimal amount,

        @NotNull(message = "A forma de pagamento é obrigatória.")
        PaymentMethod paymentMethod,

        @NotNull(message = "Informe se o pagamento foi realizado")
        Boolean paid,

        @Size(max = 50, message = "A bandeira do cartão deve ter no máximo 50 caracteres.")
        String cardBrand,

        @Size(max = 50, message = "A autorização/NSU deve ter no máximo 50 caracteres.")
        String nsuAuthorization,

        @Size(max = 100, message = "O código do boleto deve ter no máximo 100 caracteres.")
        String bankSlipCode,

        String bankSlipUrl,

        @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
        String notes
) {}
