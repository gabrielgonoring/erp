package company.aura.erp.dto.payable;

import company.aura.erp.enums.PayableOriginType;
import company.aura.erp.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayableRequest {

    private Long partnerId;

    private Long collaboratorId;

    private PayableOriginType originType;

    private Long originId;

    @NotBlank(message = "A descrição da conta a pagar é obrigatória.")
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
    private String description;

    private Integer installmentNumber;

    @NotNull(message = "A data de vencimento da parcela é obrigatória.")
    private LocalDate dueDate;

    @NotNull(message = "O valor da parcela é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor da parcela deve ser maior que zero.")
    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    @NotNull(message = "Informe se o pagamento foi realizado")
    private Boolean paid;

    private String receiptUrl;

    @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
    private String notes;
}
