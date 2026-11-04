package company.aura.erp.service;

import company.aura.erp.dto.UserResponse;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.repository.CompanyRepository;
import company.aura.erp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .companyId(10L)
                .name("Funcionario")
                .email("func@empresa.com")
                .status("ACTIVE")
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
}
