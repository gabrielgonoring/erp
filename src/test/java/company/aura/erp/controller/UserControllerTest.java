package company.aura.erp.controller;

import company.aura.erp.dto.ChangePasswordRequest;
import company.aura.erp.dto.UserCreateRequest;
import company.aura.erp.dto.UserResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("admin@empresa.com")
                .name("Admin")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("GET /api/users/me — Deve retornar os dados do usuário autenticado")
    void shouldReturnCurrentUser() {
        ResponseEntity<UserPrincipal> response = userController.getCurrentUser(userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userPrincipal, response.getBody());
    }

    @Test
    @DisplayName("GET /api/users — Deve retornar todos os usuários da companhia do usuário autenticado")
    void shouldReturnAllUsersFromCompany() {
        UserResponse user1 = new UserResponse(
                1L, 10L, "Admin", "admin@empresa.com", "ACTIVE", false, LocalDateTime.now(), LocalDateTime.now()
        );
        UserResponse user2 = new UserResponse(
                2L, 10L, "Colaborador", "colab@empresa.com", "ACTIVE", true, null, LocalDateTime.now()
        );
        List<UserResponse> expectedUsers = List.of(user1, user2);

        when(userService.findAllByCompanyId(10L)).thenReturn(expectedUsers);

        ResponseEntity<List<UserResponse>> response = userController.findAllByCompany(userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(expectedUsers, response.getBody());
        verify(userService).findAllByCompanyId(10L);
    }

    @Test
    @DisplayName("POST /api/users — Deve criar novo usuário vinculado à companhia do usuário autenticado")
    void shouldCreateUserForCompany() {
        UserCreateRequest request = new UserCreateRequest("Novo Colaborador", "novo@empresa.com", "senha123", true);
        UserResponse expectedResponse = new UserResponse(
                3L, 10L, "Novo Colaborador", "novo@empresa.com", "ACTIVE", true, null, LocalDateTime.now()
        );

        when(userService.createUserForCompany(request, 10L)).thenReturn(expectedResponse);

        ResponseEntity<UserResponse> response = userController.createUser(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(userService).createUserForCompany(request, 10L);
    }

    @Test
    @DisplayName("PUT/PATCH /api/users/me/password — Deve alterar a senha do usuário autenticado")
    void shouldChangePasswordForCurrentUser() {
        ChangePasswordRequest request = new ChangePasswordRequest("senhaAntiga123", "novaSenha456");

        ResponseEntity<Void> response = userController.changePassword(request, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).changePassword(1L, request);
    }
}
