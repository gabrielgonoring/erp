package company.aura.erp.dto.commission;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionStatementCreateRequest {

    @NotNull(message = "O ID do colaborador é obrigatório.")
    private Long collaboratorId;

    @NotNull(message = "A data inicial do período é obrigatória.")
    private LocalDate periodStart;

    @NotNull(message = "A data final do período é obrigatória.")
    private LocalDate periodEnd;

    @NotNull(message = "A data de vencimento é obrigatória.")
    private LocalDate dueDate;

    private List<Long> entryIds;

    @Size(max = 255, message = "As observações devem ter no máximo 255 caracteres.")
    private String notes;
}
