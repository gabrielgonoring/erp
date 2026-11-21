package company.aura.erp.dto.order;

import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableOriginType;
import jakarta.validation.constraints.DecimalMin;
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
public class ReceivableRequest {

    private Long orderId;

    private Long serviceOrderId;

    @NotNull(message = "Necessário informar a origem")
    private ReceivableOriginType originType;

    @NotNull(message = "Necessário informar o parceiro")
    private Long partnerId;

    @NotNull(message = "O número da parcela precisa ser informada")
    private Integer installmentNumber;

    @NotNull(message = "A data de vencimento da parcela é obrigatória.")
    private LocalDate dueDate;

    @NotNull(message = "O valor da parcela é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor da parcela deve ser maior que zero.")
    private BigDecimal amount;

    @NotNull(message = "A forma de pagamento é obrigatória.")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Informe se o pagamento foi realizado")
    private Boolean paid;

    @Size(max = 50, message = "A bandeira do cartão deve ter no máximo 50 caracteres.")
    private String cardBrand;

    @Size(max = 50, message = "A autorização/NSU deve ter no máximo 50 caracteres.")
    private String nsuAuthorization;

    @Size(max = 100, message = "O código do boleto deve ter no máximo 100 caracteres.")
    private String bankSlipCode;

    private String paymentFileUrl;

    private String paymentUrl;

    @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
    private String notes;
}
