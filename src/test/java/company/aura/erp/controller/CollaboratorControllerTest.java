package company.aura.erp.controller;

import company.aura.erp.dto.collaborator.CollaboratorCreateRequest;
import company.aura.erp.dto.collaborator.CollaboratorResponse;
import company.aura.erp.dto.collaborator.CollaboratorSummaryResponse;
import company.aura.erp.dto.collaborator.CollaboratorUpdateRequest;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CollaboratorService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaboratorControllerTest {

    @Mock
    private CollaboratorService collaboratorService;

    @InjectMocks
    private CollaboratorController collaboratorController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("POST /api/collaborators - Deve criar colaborador")
    void shouldCreateCollaborator() {
        CollaboratorCreateRequest request = CollaboratorCreateRequest.builder()
                .name("João Silva")
                .cpf("12345678900")
                .build();

        CollaboratorResponse serviceResponse = CollaboratorResponse.builder()
                .id(50L)
                .name("João Silva")
                .status("ACTIVE")
                .build();

        when(collaboratorService.create(request, 10L)).thenReturn(serviceResponse);

        ResponseEntity<CollaboratorResponse> response = collaboratorController.create(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(50L, response.getBody().getId());
        verify(collaboratorService).create(request, 10L);
    }

    @Test
    @DisplayName("PUT /api/collaborators/{id} - Deve atualizar colaborador")
    void shouldUpdateCollaborator() {
        CollaboratorUpdateRequest request = CollaboratorUpdateRequest.builder()
                .name("João Atualizado")
                .build();

        CollaboratorResponse serviceResponse = CollaboratorResponse.builder()
                .id(50L)
                .name("João Atualizado")
                .build();

        when(collaboratorService.update(50L, request, 10L)).thenReturn(serviceResponse);

        ResponseEntity<CollaboratorResponse> response = collaboratorController.update(50L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("João Atualizado", response.getBody().getName());
        verify(collaboratorService).update(50L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/collaborators/{id} - Deve remover colaborador e retornar NoContent")
    void shouldDeleteCollaborator() {
        ResponseEntity<Void> response = collaboratorController.delete(50L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(collaboratorService).delete(50L, 10L);
    }



    @Test
    @DisplayName("GET /api/collaborators/{id} - Deve buscar por ID")
    void shouldFindById() {
        CollaboratorResponse serviceResponse = CollaboratorResponse.builder()
                .id(50L)
                .name("João")
                .build();

        when(collaboratorService.findById(50L, 10L)).thenReturn(serviceResponse);

        ResponseEntity<CollaboratorResponse> response = collaboratorController.findById(50L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(50L, response.getBody().getId());
    }
}
