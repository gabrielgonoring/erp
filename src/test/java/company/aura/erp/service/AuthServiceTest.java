package company.aura.erp.service;

import company.aura.erp.constants.AuthConstants;
import company.aura.erp.dto.auth.ForgotPasswordRequest;
import company.aura.erp.dto.auth.ResetPasswordRequest;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.repository.UserRepository;
import company.aura.erp.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Mock
    private AuthConstants authConstants;



    @Test
    @DisplayName("Deve gerar código de 255 caracteres, salvar expiração e disparar e-mail com sucesso")
    void shouldRequestPasswordResetSuccessfully() {
        String email = "usuario@empresa.com";
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email(email)
                .name("Usuario Teste")
                .passwordHash("senha_antiga")
                .status("ACTIVE")
                .build();

        when(authConstants.getResetPasswordBaseUrl()).thenReturn("http://localhost:5173/redefinir-senha");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        authService.requestPasswordReset(request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();

        assertNotNull(savedUser.getResetPasswordToken());
        assertEquals(255, savedUser.getResetPasswordToken().length(), "O token gerado deve ter exatamente 255 caracteres");
        assertNotNull(savedUser.getResetPasswordExpires());
        assertTrue(savedUser.getResetPasswordExpires().isAfter(LocalDateTime.now()));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(user), urlCaptor.capture());
        String generatedUrl = urlCaptor.getValue();

        assertTrue(generatedUrl.startsWith("http://localhost:5173/redefinir-senha?code="));
        assertEquals("http://localhost:5173/redefinir-senha?code=" + savedUser.getResetPasswordToken(), generatedUrl);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 quando o e-mail não for encontrado")
    void shouldThrowNotFoundWhenEmailDoesNotExist() {
        String email = "inexistente@empresa.com";
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        when(authConstants.getResetPasswordBaseUrl()).thenReturn("http://localhost:5173/redefinir-senha");
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.requestPasswordReset(request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
    }

    @Test
    @DisplayName("Deve redefinir senha com sucesso quando o código for válido e não expirado")
    void shouldResetPasswordSuccessfully() {
        String code = "a".repeat(255);
        String newPassword = "novaSenhaSegura123";
        ResetPasswordRequest request = new ResetPasswordRequest(code, newPassword);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("usuario@empresa.com")
                .passwordHash("senha_antiga")
                .resetPasswordToken(code)
                .resetPasswordExpires(LocalDateTime.now().plusMinutes(30))
                .build();
        when(authConstants.getResetPasswordBaseUrl()).thenReturn("http://localhost:5173/redefinir-senha");
        when(userRepository.findByResetPasswordToken(code)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("nova_senha_hasheada");

        authService.resetPassword(request);

        assertEquals("nova_senha_hasheada", user.getPasswordHash());
        assertNull(user.getResetPasswordToken());
        assertNull(user.getResetPasswordExpires());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deve lançar erro 400 quando o código for inexistente")
    void shouldThrowBadRequestWhenCodeNotFound() {
        String code = "b".repeat(255);
        ResetPasswordRequest request = new ResetPasswordRequest(code, "novaSenha123");

        when(userRepository.findByResetPasswordToken(code)).thenReturn(Optional.empty());
        when(authConstants.getResetPasswordBaseUrl()).thenReturn("http://localhost:5173/redefinir-senha");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar erro 400 quando o código estiver expirado")
    void shouldThrowBadRequestWhenCodeIsExpired() {
        String code = "c".repeat(255);
        ResetPasswordRequest request = new ResetPasswordRequest(code, "novaSenha123");

        UserEntity user = UserEntity.builder()
                .id(1L)
                .email("usuario@empresa.com")
                .passwordHash("senha_antiga")
                .resetPasswordToken(code)
                .resetPasswordExpires(LocalDateTime.now().minusMinutes(5))
                .build();

        when(userRepository.findByResetPasswordToken(code)).thenReturn(Optional.of(user));
        when(authConstants.getResetPasswordBaseUrl()).thenReturn("http://localhost:5173/redefinir-senha");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
}
