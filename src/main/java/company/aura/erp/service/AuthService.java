package company.aura.erp.service;

import company.aura.erp.dto.auth.LoginRequest;
import company.aura.erp.dto.auth.LoginResponse;
import company.aura.erp.dto.UserResponse;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.repository.UserRepository;
import company.aura.erp.security.JwtService;
import company.aura.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + request.email()));

        userService.recordLogin(user.getEmail());

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtService.generateToken(principal);

        return LoginResponse.of(token, jwtService.getExpirationTime(), UserResponse.fromEntity(user));
    }
}
