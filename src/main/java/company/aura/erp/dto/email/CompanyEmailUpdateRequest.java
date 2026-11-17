package company.aura.erp.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyEmailUpdateRequest(
        @Size(max = 60, message = "O tipo de notificação deve ter no máximo 60 caracteres.")
        String notificationType,

        @Email(message = "O e-mail do remetente é inválido.")
        @Size(max = 255, message = "O e-mail do remetente deve ter no máximo 255 caracteres.")
        String fromEmail,

        @NotBlank(message = "O nome do remetente é obrigatório.")
        @Size(max = 255, message = "O nome do remetente deve ter no máximo 255 caracteres.")
        String fromName,

        @Email(message = "O e-mail de resposta (reply-to) é inválido.")
        @Size(max = 255, message = "O e-mail de resposta (reply-to) deve ter no máximo 255 caracteres.")
        String replyTo
) {
}
