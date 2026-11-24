package company.aura.erp.dto.commission;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
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
public class CommissionRuleCreateRequest {

    private Long collaboratorId;

    private Long categoryId;

    private Long catalogItemId;

    @NotNull(message = "O percentual de comissão é obrigatório.")
    @DecimalMin(value = "0.00", message = "O percentual de comissão não pode ser negativo.")
    @DecimalMax(value = "100.00", message = "O percentual de comissão não pode ultrapassar 100%.")
    @Digits(integer = 3, fraction = 2, message = "O percentual de comissão deve ter no máximo 3 dígitos inteiros e 2 decimais.")
    private BigDecimal commissionPercentage;

    private LocalDate validFrom;

    private LocalDate validUntil;
}
