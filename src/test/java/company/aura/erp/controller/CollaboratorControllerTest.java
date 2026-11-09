package company.aura.erp.controller;

import company.aura.erp.dto.collaborator.*;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CollaboratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                .defaultCommissionPercentage(new BigDecimal("10.00"))
                .build();

        CollaboratorResponse serviceResponse = CollaboratorResponse.builder()
                .id(50L)
                .name("João Silva")
                .defaultCommissionPercentage(new BigDecimal("10.00"))
                .status("ACTIVE")
                .build();

        when(collaboratorService.create(request, 10L)).thenReturn(serviceResponse);

        ResponseEntity<CollaboratorResponse> response = collaboratorController.create(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(50L, response.getBody().getId());
        assertEquals(new BigDecimal("10.00"), response.getBody().getDefaultCommissionPercentage());
        verify(collaboratorService).create(request, 10L);
    }

    @Test
    @DisplayName("PUT /api/collaborators/{id} - Deve atualizar colaborador")
    void shouldUpdateCollaborator() {
        CollaboratorUpdateRequest request = CollaboratorUpdateRequest.builder()
                .name("João Atualizado")
                .defaultCommissionPercentage(new BigDecimal("15.00"))
                .build();

        CollaboratorResponse serviceResponse = CollaboratorResponse.builder()
                .id(50L)
                .name("João Atualizado")
                .defaultCommissionPercentage(new BigDecimal("15.00"))
                .build();

        when(collaboratorService.update(50L, request, 10L)).thenReturn(serviceResponse);

        ResponseEntity<CollaboratorResponse> response = collaboratorController.update(50L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("João Atualizado", response.getBody().getName());
        assertEquals(new BigDecimal("15.00"), response.getBody().getDefaultCommissionPercentage());
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

    @Test
    @DisplayName("GET /api/collaborators/{id}/order-items - Deve retornar itens vendidos com sucesso")
    void shouldGetSoldItemsByPathId() {
        CollaboratorCommissionReportResponse report = CollaboratorCommissionReportResponse.builder()
                .collaboratorId(50L)
                .collaboratorName("João Silva")
                .header(CollaboratorCommissionReportHeader.builder()
                        .startDate(LocalDateTime.of(2026, 11, 1, 0, 0))
                        .endDate(LocalDateTime.of(2026, 11, 8, 23, 59, 59))
                        .totalSold(new BigDecimal("500.00"))
                        .totalCommission(new BigDecimal("50.00"))
                        .commissionPercentage(new BigDecimal("10.00"))
                        .build())
                .items(List.of(CollaboratorSoldItemResponse.builder()
                        .itemId(1L)
                        .originalOrderId(100L)
                        .unitPrice(new BigDecimal("250.00"))
                        .commissionAmount(new BigDecimal("25.00"))
                        .description("Item Teste")
                        .quantity(new BigDecimal("2.0000"))
                        .totalPrice(new BigDecimal("500.00"))
                        .date(LocalDateTime.of(2026, 11, 2, 10, 0))
                        .build()))
                .build();

        when(collaboratorService.getSoldItemsReport(eq(50L), any(LocalDateTime.class), any(LocalDateTime.class), eq(10L)))
                .thenReturn(report);

        ResponseEntity<CollaboratorCommissionReportResponse> response = collaboratorController.getSoldItems(
                50L, "2026-11-01", "2026-11-08", null, null, null, null, null, null, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(50L, response.getBody().getCollaboratorId());
        assertEquals(new BigDecimal("500.00"), response.getBody().getHeader().getTotalSold());
        assertEquals(new BigDecimal("50.00"), response.getBody().getHeader().getTotalCommission());
        assertEquals(1, response.getBody().getItems().size());
    }

    @Test
    @DisplayName("GET /api/collaborators/order-items - Deve retornar itens vendidos via query param")
    void shouldGetSoldItemsByQueryParam() {
        CollaboratorCommissionReportResponse report = CollaboratorCommissionReportResponse.builder()
                .collaboratorId(50L)
                .header(CollaboratorCommissionReportHeader.builder()
                        .totalSold(BigDecimal.TEN)
                        .totalCommission(BigDecimal.ONE)
                        .build())
                .items(Collections.emptyList())
                .build();

        when(collaboratorService.getSoldItemsReport(eq(50L), any(LocalDateTime.class), any(LocalDateTime.class), eq(10L)))
                .thenReturn(report);

        ResponseEntity<CollaboratorCommissionReportResponse> response = collaboratorController.getSoldItemsByParam(
                50L, null, "2026-11-01T00:00:00", "2026-11-08T23:59:59", null, null, null, null, null, null, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(50L, response.getBody().getCollaboratorId());
    }

    @Test
    @DisplayName("GET /api/collaborators/{id}/order-items - Deve falhar quando data inicial está ausente")
    void shouldFailWhenStartDateMissing() {
        assertThrows(ResponseStatusException.class, () ->
                collaboratorController.getSoldItems(50L, null, "2026-11-08", null, null, null, null, null, null, userPrincipal));
    }

    @Test
    @DisplayName("GET /api/collaborators/{id}/order-items - Deve falhar quando data final está ausente")
    void shouldFailWhenEndDateMissing() {
        assertThrows(ResponseStatusException.class, () ->
                collaboratorController.getSoldItems(50L, "2026-11-01", null, null, null, null, null, null, null, userPrincipal));
    }

    @Test
    @DisplayName("GET /api/collaborators/order-items - Deve falhar quando ID do colaborador está ausente")
    void shouldFailWhenCollaboratorIdMissingInParam() {
        assertThrows(ResponseStatusException.class, () ->
                collaboratorController.getSoldItemsByParam(null, null, "2026-11-01", "2026-11-08", null, null, null, null, null, null, userPrincipal));
    }
}
