package company.aura.erp.dto.order;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record OrderCloseRequest(
        @NotEmpty(message = "Informe ao menos uma parcela de pagamento.")
        List<@Valid OrderInstallmentRequest> installments,

        @Valid
        OrderShippingRequest shipping
) {
    public OrderCloseRequest(List<@Valid OrderInstallmentRequest> installments) {
        this(installments, null);
    }
}
