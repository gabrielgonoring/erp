package company.aura.erp.service;

import company.aura.erp.dto.ChangePasswordRequest;
import company.aura.erp.dto.UserCreateRequest;
import company.aura.erp.dto.UserResponse;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.repository.CompanyRepository;
import company.aura.erp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Deve buscar todos os usuários por ID da companhia")
    void shouldFindAllUsersByCompanyId() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .companyId(10L)
                .name("Admin")
                .email("admin@empresa.com")
                .status("ACTIVE")
                .temporaryPassword(false)
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .companyId(10L)
                .name("Funcionario")
                .email("func@empresa.com")
                .status("ACTIVE")
                .temporaryPassword(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByCompanyId(10L)).thenReturn(List.of(user1, user2));

        List<UserResponse> result = userService.findAllByCompanyId(10L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Admin", result.get(0).name());
        assertEquals("Funcionario", result.get(1).name());
        verify(userRepository).findByCompanyId(10L);
    }

    @Test
    @DisplayName("Deve retornar lista vazia se companyId for nulo")
    void shouldReturnEmptyListIfCompanyIdIsNull() {
        List<UserResponse> result = userService.findAllByCompanyId(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Deve criar novo usuário vinculado à empresa com flag de senha temporária")
    void shouldCreateUserForCompanyWithTemporaryPassword() {
        UserCreateRequest request = new UserCreateRequest("Novo Colab", "novo@empresa.com", "senhaTemp123", true);

        when(userRepository.existsByEmail("novo@empresa.com")).thenReturn(false);
        when(passwordEncoder.encode("senhaTemp123")).thenReturn("encodedPassword");

        UserEntity savedEntity = UserEntity.builder()
                .id(100L)
                .companyId(10L)
                .name(request.name())
                .email(request.email())
                .passwordHash("encodedPassword")
                .status("ACTIVE")
                .temporaryPassword(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        UserResponse response = userService.createUserForCompany(request, 10L);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(10L, response.companyId());
        assertEquals("novo@empresa.com", response.email());
        assertTrue(response.temporaryPassword());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity captured = captor.getValue();
        assertEquals(10L, captured.getCompanyId());
        assertEquals("encodedPassword", captured.getPasswordHash());
        assertTrue(captured.isTemporaryPassword());
        assertEquals("ACTIVE", captured.getStatus());
    }

    @Test
    @DisplayName("Deve lançar erro ao criar usuário se companyId for nulo")
    void shouldThrowWhenCompanyIdIsNullOnCreateUser() {
        UserCreateRequest request = new UserCreateRequest("Nome", "email@teste.com", "senha123", false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUserForCompany(request, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar erro se e-mail já estiver cadastrado")
    void shouldThrowWhenEmailAlreadyExistsOnCreateUser() {
        UserCreateRequest request = new UserCreateRequest("Nome", "duplicado@teste.com", "senha123", false);
        when(userRepository.existsByEmail("duplicado@teste.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUserForCompany(request, 10L)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve alterar senha do usuário com sucesso e definir temporaryPassword como false")
    void shouldChangePasswordSuccessfully() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .companyId(10L)
                .email("user@empresa.com")
                .passwordHash("oldEncodedHash")
                .temporaryPassword(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPass", "oldEncodedHash")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newEncodedHash");

        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "newPass123");
        userService.changePassword(1L, request);

        assertEquals("newEncodedHash", user.getPasswordHash());
        assertFalse(user.isTemporaryPassword());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deve lançar erro se a senha atual informada for incorreta")
    void shouldThrowIfCurrentPasswordIsIncorrect() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .passwordHash("oldEncodedHash")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "oldEncodedHash")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass123");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.changePassword(1L, request)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar erro se usuário não for encontrado ao alterar senha")
    void shouldThrowIfUserNotFoundOnChangePassword() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ChangePasswordRequest request = new ChangePasswordRequest(null, "newPass123");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.changePassword(999L, request)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
