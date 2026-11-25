package company.aura.erp.dto.order;

import company.aura.erp.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivableUpdateRequest {

    @NotNull(message = "Necessário informar o parceiro")
    private Long partnerId;

    @NotNull(message = "O número da parcela precisa ser informada")
    @Min(value = 1, message = "O número da parcela deve ser maior que zero.")
    private Integer installmentNumber;

    @NotNull(message = "A data de vencimento da parcela é obrigatória.")
    private LocalDate dueDate;

    @NotNull(message = "A forma de pagamento é obrigatória.")
    private PaymentMethod paymentMethod;

    @Size(max = 100, message = "O código do boleto deve ter no máximo 100 caracteres.")
    private String bankSlipCode;

    private String paymentFileUrl;

    private String paymentUrl;

    @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
    private String notes;
}
