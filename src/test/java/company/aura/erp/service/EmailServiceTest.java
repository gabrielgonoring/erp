package company.aura.erp.service;

import company.aura.erp.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private AmazonSesService amazonSesService;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("Deve enviar e-mail de recuperação de senha com sucesso")
    void shouldSendPasswordResetEmail() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .name("Gabriel Borges")
                .email("gabriel@aura.company")
                .build();

        String resetUrl = "https://erp.aura.company/redefinir-senha?token=abc-123";

        when(amazonSesService.sendEmail(
                eq("gabriel@aura.company"),
                eq("Redefinição de Senha - Aura ERP"),
                contains(resetUrl),
                contains(resetUrl)
        )).thenReturn("msg-reset-123");

        emailService.sendPasswordResetEmail(user, resetUrl);

        verify(amazonSesService, times(1)).sendEmail(
                eq("gabriel@aura.company"),
                eq("Redefinição de Senha - Aura ERP"),
                contains(resetUrl),
                contains(resetUrl)
        );
    }

    @Test
    @DisplayName("Não deve tentar enviar se o usuário ou e-mail for nulo/vazio")
    void shouldNotSendIfUserOrEmailIsInvalid() {
        emailService.sendPasswordResetEmail(null, "http://localhost");
        emailService.sendPasswordResetEmail(UserEntity.builder().email(null).build(), "http://localhost");
        emailService.sendPasswordResetEmail(UserEntity.builder().email("   ").build(), "http://localhost");

        verifyNoInteractions(amazonSesService);
    }
}
