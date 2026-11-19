package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.ServiceOrderUpdateEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderUpdateEditRequest {

    @NotNull(message = "O tipo do evento é obrigatório.")
    private ServiceOrderUpdateEventType eventType;

    @NotBlank(message = "A descrição da atualização é obrigatória.")
    private String description;
}
