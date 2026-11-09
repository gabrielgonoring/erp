package company.aura.erp.service;

import company.aura.erp.dto.collaborator.CollaboratorCreateRequest;
import company.aura.erp.dto.collaborator.CollaboratorResponse;
import company.aura.erp.dto.collaborator.CollaboratorSummaryResponse;
import company.aura.erp.dto.collaborator.CollaboratorUpdateRequest;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollaboratorServiceTest {

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CollaboratorService collaboratorService;

    private Long companyId;
    private CollaboratorEntity collaborator;

    @BeforeEach
    void setUp() {
        companyId = 1L;
        collaborator = CollaboratorEntity.builder()
                .id(10L)
                .companyId(companyId)
                .userId(100L)
                .name("Maria Silva")
                .cpf("123.456.789-00")
                .email("maria.silva@email.com")
                .phone("11999998888")
                .status("ACTIVE")
                .jobTitle("Desenvolvedora")
                .build();
    }

    @Test
    @DisplayName("Deve criar colaborador com sucesso")
    void shouldCreateCollaboratorSuccessfully() {
        CollaboratorCreateRequest request = CollaboratorCreateRequest.builder()
                .name("Maria Silva")
                .userId(100L)
                .cpf("123.456.789-00")
                .email("maria.silva@email.com")
                .phone("11999998888")
                .jobTitle("Desenvolvedora")
                .build();

        UserEntity user = UserEntity.builder()
                .id(100L)
                .companyId(companyId)
                .name("Maria User")
                .status("ACTIVE")
                .build();

        when(userRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(user));
        when(collaboratorRepository.save(any(CollaboratorEntity.class))).thenReturn(collaborator);

        CollaboratorResponse response = collaboratorService.create(request, companyId);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Maria Silva", response.getName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(100L, response.getUserId());
        verify(collaboratorRepository).save(any(CollaboratorEntity.class));
    }

    @Test
    @DisplayName("Deve falhar ao criar colaborador se usuário vinculado não for encontrado")
    void shouldFailWhenLinkedUserNotFound() {
        CollaboratorCreateRequest request = CollaboratorCreateRequest.builder()
                .name("Maria Silva")
                .userId(999L)
                .build();

        when(userRepository.findByIdAndCompanyId(999L, companyId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> collaboratorService.create(request, companyId));
        verify(collaboratorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar colaborador com sucesso")
    void shouldUpdateCollaboratorSuccessfully() {
        CollaboratorUpdateRequest request = CollaboratorUpdateRequest.builder()
                .name("Maria Santos")
                .userId(100L)
                .cpf("123.456.789-00")
                .email("maria.santos@email.com")
                .phone("11888887777")
                .build();

        UserEntity user = UserEntity.builder()
                .id(100L)
                .companyId(companyId)
                .status("ACTIVE")
                .build();

        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(10L, companyId, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));
        when(userRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(user));
        when(collaboratorRepository.save(any(CollaboratorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CollaboratorResponse response = collaboratorService.update(10L, request, companyId);

        assertNotNull(response);
        assertEquals("Maria Santos", response.getName());
        assertEquals("maria.santos@email.com", response.getEmail());
        verify(collaboratorRepository).save(collaborator);
    }

    @Test
    @DisplayName("Deve apagar colaborador alterando status para DELETED")
    void shouldDeleteCollaboratorBySettingStatusToDeleted() {
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(10L, companyId, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));

        collaboratorService.delete(10L, companyId);

        assertEquals(CollaboratorStatus.DELETED.name(), collaborator.getStatus());
        verify(collaboratorRepository).save(collaborator);
    }

    @Test
    @DisplayName("Deve falhar ao apagar colaborador não encontrado")
    void shouldFailToDeleteNonExistentCollaborator() {
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(99L, companyId, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> collaboratorService.delete(99L, companyId));
        verify(collaboratorRepository, never()).save(any());
    }



    @Test
    @DisplayName("Deve listar colaboradores incluindo os que estão com status DELETED")
    void shouldListCollaboratorsIncludingDeleted() {
        Pageable pageable = PageRequest.of(0, 10);
        CollaboratorEntity deletedColab = CollaboratorEntity.builder()
                .id(11L)
                .companyId(companyId)
                .name("Carlos Apagado")
                .cpf("999.888.777-66")
                .email("carlos@email.com")
                .status("DELETED")
                .build();

        Page<CollaboratorEntity> page = new PageImpl<>(List.of(collaborator, deletedColab));

        when(collaboratorRepository.searchByCompanyIdAndTerm(eq(companyId), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<CollaboratorSummaryResponse> result = collaboratorService.listPaginated(null, null, companyId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("ACTIVE", result.getContent().get(0).getStatus());
        assertEquals("DELETED", result.getContent().get(1).getStatus());
        assertEquals("Carlos Apagado", result.getContent().get(1).getNome());
    }

    @Test
    @DisplayName("Deve buscar colaborador por ID com sucesso")
    void shouldFindByIdSuccessfully() {
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(10L, companyId, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));

        CollaboratorResponse response = collaboratorService.findById(10L, companyId);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Maria Silva", response.getName());
    }
}
