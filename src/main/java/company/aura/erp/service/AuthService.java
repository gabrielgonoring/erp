package company.aura.erp.service;

import company.aura.erp.constants.AuthConstants;
import company.aura.erp.dto.UserResponse;
import company.aura.erp.dto.auth.ForgotPasswordRequest;
import company.aura.erp.dto.auth.LoginRequest;
import company.aura.erp.dto.auth.LoginResponse;
import company.aura.erp.dto.auth.ResetPasswordRequest;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.enums.UserStatus;
import company.aura.erp.repository.UserRepository;
import company.aura.erp.security.JwtService;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthConstants authConstants;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + request.email()));

        if (user.getStatus() != null && !UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo ou desativado.");
        }

        userService.recordLogin(user.getEmail());

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtService.generateToken(principal);

        return LoginResponse.of(token, jwtService.getExpirationTime(), UserResponse.fromEntity(user));
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + request.email()));

        if (user.getStatus() != null && !UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo ou desativado.");
        }

        String code = SecurityUtil.generateResetCode();
        user.setResetPasswordToken(code);
        user.setResetPasswordExpires(LocalDateTime.now().plusMinutes(authConstants.getResetPasswordExpirationMinutes()));
        userRepository.save(user);

        String resetUrl = authConstants.getResetPasswordBaseUrl() + "?code=" + code;
        emailService.sendPasswordResetEmail(user, resetUrl);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = userRepository.findByResetPasswordToken(request.code())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de redefinição de senha inválido ou inexistente."));

        if (user.getStatus() != null && !UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo ou desativado.");
        }

        if (user.getResetPasswordExpires() == null || user.getResetPasswordExpires().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de redefinição de senha expirado.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpires(null);
        userRepository.save(user);
    }
}
