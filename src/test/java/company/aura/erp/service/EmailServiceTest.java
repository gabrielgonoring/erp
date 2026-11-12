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
    @DisplayName("Não deve tentar enviar se o usuário ou e-mail for nulo/vazio")
    void shouldNotSendIfUserOrEmailIsInvalid() {
        emailService.sendPasswordResetEmail(null, "http://localhost");
        emailService.sendPasswordResetEmail(UserEntity.builder().email(null).build(), "http://localhost");
        emailService.sendPasswordResetEmail(UserEntity.builder().email("   ").build(), "http://localhost");

        verifyNoInteractions(amazonSesService);
    }
}
