package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.ServiceOrderTermStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderTermCreateRequest {

    @NotBlank(message = "O título do termo é obrigatório.")
    @Size(max = 100, message = "O título do termo deve ter no máximo 100 caracteres.")
    private String title;

    @NotBlank(message = "O conteúdo do termo é obrigatório.")
    private String content;

    private Boolean isDefault;
}
