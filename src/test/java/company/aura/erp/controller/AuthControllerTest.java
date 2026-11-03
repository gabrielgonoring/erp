package company.aura.erp.controller;

import company.aura.erp.dto.UserRegistrationRequest;
import company.aura.erp.dto.UserResponse;
import company.aura.erp.dto.auth.ForgotPasswordRequest;
import company.aura.erp.dto.auth.LoginRequest;
import company.aura.erp.dto.auth.LoginResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Deve chamar authService.login e retornar HTTP 200 com LoginResponse")
    void shouldHandleLogin() {
        LoginRequest request = new LoginRequest("teste@empresa.com", "senha123");
        LoginResponse expectedResponse = new LoginResponse("token", "Bearer", 3600L, null);
        when(authService.login(request)).thenReturn(expectedResponse);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(authService).login(request);
    }

    @Test
    @DisplayName("Deve chamar userService.registerUser e retornar HTTP 201 com UserResponse")
    void shouldHandleRegister() {
        UserRegistrationRequest request = new UserRegistrationRequest("Nome", "teste@empresa.com", "senha123");
        UserResponse expectedResponse = new UserResponse(1L, 10L, "Nome", "teste@empresa.com", "ACTIVE", null, null);
        when(userService.registerUser(request)).thenReturn(expectedResponse);

        ResponseEntity<UserResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(userService).registerUser(request);
    }

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
