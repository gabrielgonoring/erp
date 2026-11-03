package company.aura.erp.controller;

import company.aura.erp.dto.auth.ForgotPasswordRequest;
import company.aura.erp.dto.auth.ResetPasswordRequest;
import company.aura.erp.service.AuthService;
import company.aura.erp.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Deve chamar authService.requestPasswordReset e retornar HTTP 200")
    void shouldHandleForgotPassword() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("teste@empresa.com");

        ResponseEntity<Void> response = authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).requestPasswordReset(request);
    }

    @Test
    @DisplayName("Deve chamar authService.resetPassword e retornar HTTP 200")
    void shouldHandleResetPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest("a".repeat(255), "novaSenhaSegura");

        ResponseEntity<Void> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).resetPassword(request);
    }
}
