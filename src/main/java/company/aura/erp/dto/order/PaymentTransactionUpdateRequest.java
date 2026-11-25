package company.aura.erp.dto.order;

import company.aura.erp.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionUpdateRequest {

    @NotNull(message = "A forma de pagamento é obrigatória.")
    private PaymentMethod paymentMethod;

    private LocalDateTime paidAt;

    @Size(max = 50, message = "A bandeira do cartão deve ter no máximo 50 caracteres.")
    private String cardBrand;

    @Size(max = 50, message = "A autorização/NSU deve ter no máximo 50 caracteres.")
    private String nsuAuthorization;

    @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
    private String notes;
}
