package company.aura.erp.service;

import company.aura.erp.dto.ChangePasswordRequest;
import company.aura.erp.dto.UserCreateRequest;
import company.aura.erp.dto.UserRegistrationRequest;
import company.aura.erp.dto.UserResponse;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.enums.UserStatus;
import company.aura.erp.repository.UserRepository;
import company.aura.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("registerUser() - START - email={}, name={}", request != null ? request.email() : null, request != null ? request.name() : null);

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado: " + request.email());
        }

        UserEntity user = UserEntity.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE.name())
                .temporaryPassword(false)
                .build();

        UserEntity savedUser = userRepository.save(user);
        UserResponse response = UserResponse.fromEntity(savedUser);

        log.info("registerUser() = END - userId={}, email={}", response.id(), response.email());
        return response;
    }

    @Transactional
    public UserResponse createUserForCompany(UserCreateRequest request, Long companyId) {
        log.info("createUserForCompany() - START - companyId={}, email={}", companyId, request != null ? request.email() : null);

        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário logado não possui uma empresa vinculada.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado: " + request.email());
        }

        boolean isTemp = request.temporaryPassword() != null ? request.temporaryPassword() : true;

        UserEntity user = UserEntity.builder()
                .companyId(companyId)
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE.name())
                .temporaryPassword(isTemp)
                .build();

        UserEntity savedUser = userRepository.save(user);
        UserResponse response = UserResponse.fromEntity(savedUser);

        log.info("createUserForCompany() = END - userId={}, email={}, temporaryPassword={}", response.id(), response.email(), isTemp);
        return response;
    }

    @Transactional
    public void deleteUser(Long id, UserPrincipal currentUser) {
        log.info("deleteUser() - START - id={}, currentUserId={}", id, currentUser.getId());

        if (id.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é permitido apagar o próprio usuário logado.");
        }

        if (currentUser.getCompanyId()==null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seu usuário não está vinculado a uma empresa.");
        }


        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        if (!user.getCompanyId().equals(currentUser.getCompanyId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esse usuário não pertence a sua empresa");
        }

        user.setStatus(UserStatus.DELETED.name());
        userRepository.save(user);

        log.info("deleteUser() = END - id={}, status={}", id, user.getStatus());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("changePassword() - START - userId={}", userId);

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        if (user.getStatus() != null && !UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário inativo ou desativado.");
        }

        if (request.currentPassword() != null && !request.currentPassword().isBlank()) {
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual informada está incorreta.");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTemporaryPassword(false);
        userRepository.save(user);

        log.info("changePassword() = END - userId={}", userId);
    }

    @Transactional
    public void recordLogin(String email) {
        log.info("recordLogin() - START - email={}", email);

        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() == null || UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
            }
        });

        log.info("recordLogin() = END - email={}", email);
    }

    public List<UserResponse> findAllByCompanyId(Long companyId, String status) {
        log.info("findAllByCompanyId() - START - companyId={}, status={}", companyId, status);

        if (companyId == null) {
            log.info("findAllByCompanyId() = END - totalUsers=0");
            return Collections.emptyList();
        }

        List<UserResponse> users = userRepository.findByCompanyId(companyId).stream()
                .map(UserResponse::fromEntity)
                .toList();

        log.info("findAllByCompanyId() = END - companyId={}, status={}, totalUsers={}", companyId, status, users.size());
        return users;
    }

    @Transactional(readOnly = true)
    public UserResponse findUserById(Long userId) {
        log.info("findUserById() - START - userId={}", userId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getStatus() != null && !UserStatus.ACTIVE.name().equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        UserResponse userResponse = UserResponse.fromEntity(user);

        log.info("findUserById() = END - userId={}, user: {}", userId, userResponse);
        return userResponse;
    }
}
