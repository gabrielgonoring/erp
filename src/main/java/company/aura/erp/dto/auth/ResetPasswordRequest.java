package company.aura.erp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "O código é obrigatório.")
        @Size(min = 255, max = 255, message = "O código deve conter exatamente 255 caracteres.")
        String code,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 6, message = "A nova senha deve ter no mínimo 6 caracteres.")
        String newPassword
) {}
