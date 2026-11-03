package company.aura.erp.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderCloseRequest(
        @NotEmpty(message = "Informe ao menos uma parcela de pagamento.")
        List<@Valid OrderInstallmentRequest> installments
) {}
