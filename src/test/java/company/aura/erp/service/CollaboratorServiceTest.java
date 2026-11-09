package company.aura.erp.service;

import company.aura.erp.dto.collaborator.*;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.OrderItemRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Mock
    private OrderItemRepository orderItemRepository;

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
                .defaultCommissionPercentage(new BigDecimal("10.00"))
                .build();
    }

    @Test
    @DisplayName("Deve criar colaborador com sucesso incluindo porcentagem de comissão")
    void shouldCreateCollaboratorSuccessfully() {
        CollaboratorCreateRequest request = CollaboratorCreateRequest.builder()
                .name("Maria Silva")
                .userId(100L)
                .cpf("123.456.789-00")
                .email("maria.silva@email.com")
                .phone("11999998888")
                .jobTitle("Desenvolvedora")
                .defaultCommissionPercentage(new BigDecimal("10.00"))
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
        assertEquals(new BigDecimal("10.00"), response.getDefaultCommissionPercentage());
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
    @DisplayName("Deve atualizar colaborador com sucesso incluindo comissão")
    void shouldUpdateCollaboratorSuccessfully() {
        CollaboratorUpdateRequest request = CollaboratorUpdateRequest.builder()
                .name("Maria Santos")
                .userId(100L)
                .cpf("123.456.789-00")
                .email("maria.santos@email.com")
                .phone("11888887777")
                .defaultCommissionPercentage(new BigDecimal("12.50"))
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
        assertEquals(new BigDecimal("12.50"), response.getDefaultCommissionPercentage());
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

    @Test
    @DisplayName("Deve gerar relatório de itens vendidos com comissão calculada")
    void shouldGenerateSoldItemsReportSuccessfully() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 8, 23, 59, 59);

        OrderItemEntity item1 = OrderItemEntity.builder()
                .id(101L)
                .orderId(501L)
                .professionalId(10L)
                .itemType(CatalogItemType.SERVICE)
                .description("Corte de Cabelo")
                .quantity(new BigDecimal("2.0000"))
                .unitPrice(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("200.00"))
                .createdAt(LocalDateTime.of(2026, 11, 3, 14, 30))
                .build();

        OrderItemEntity item2 = OrderItemEntity.builder()
                .id(102L)
                .orderId(502L)
                .professionalId(10L)
                .itemType(CatalogItemType.PHYSICAL)
                .description("Pomada Modeladora")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("50.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("50.00"))
                .createdAt(LocalDateTime.of(2026, 11, 5, 10, 0))
                .build();

        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(10L, companyId, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));
        when(orderItemRepository.findByCollaboratorAndCompanyAndCreatedAtBetween(10L, companyId, start, end))
                .thenReturn(List.of(item1, item2));

        CollaboratorCommissionReportResponse response = collaboratorService.getSoldItemsReport(10L, start, end, companyId);

        assertNotNull(response);
        assertEquals(10L, response.getCollaboratorId());
        assertEquals("Maria Silva", response.getCollaboratorName());

        assertNotNull(response.getHeader());
        assertEquals(start, response.getHeader().getStartDate());
        assertEquals(end, response.getHeader().getEndDate());
        assertEquals(new BigDecimal("250.00"), response.getHeader().getTotalSold());
        // 10% commission on 250.00 = 25.00
        assertEquals(new BigDecimal("25.00"), response.getHeader().getTotalCommission());
        assertEquals(new BigDecimal("10.00"), response.getHeader().getCommissionPercentage());

        // Alias checks
        assertEquals(new BigDecimal("250.00"), response.getValorTotalVendido());
        assertEquals(new BigDecimal("25.00"), response.getValorComissao());

        assertEquals(2, response.getItems().size());

        CollaboratorSoldItemResponse itemResponse1 = response.getItems().get(0);
        assertEquals(101L, itemResponse1.getItemId());
        assertEquals(501L, itemResponse1.getOriginalOrderId());
        assertEquals(501L, itemResponse1.getIdPedidoOriginal());
        assertEquals(new BigDecimal("100.00"), itemResponse1.getUnitPrice());
        assertEquals(new BigDecimal("100.00"), itemResponse1.getValorItem());
        assertEquals(new BigDecimal("20.00"), itemResponse1.getCommissionAmount());
        assertEquals(new BigDecimal("20.00"), itemResponse1.getValorComissao());
        assertEquals("Corte de Cabelo", itemResponse1.getDescription());
        assertEquals("Corte de Cabelo", itemResponse1.getDescricaoItem());
        assertEquals(new BigDecimal("2.0000"), itemResponse1.getQuantity());
        assertEquals(new BigDecimal("2.0000"), itemResponse1.getQuantidade());
        assertEquals(new BigDecimal("200.00"), itemResponse1.getTotalPrice());
        assertEquals(new BigDecimal("200.00"), itemResponse1.getValorTotalItem());
    }

    @Test
    @DisplayName("Deve falhar ao buscar relatório com ID nulo")
    void shouldFailReportWhenCollaboratorIdIsNull() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        assertThrows(ResponseStatusException.class, () ->
                collaboratorService.getSoldItemsReport(null, start, end, companyId));
    }

    @Test
    @DisplayName("Deve falhar ao buscar relatório com data inicial nula")
    void shouldFailReportWhenStartDateIsNull() {
        LocalDateTime end = LocalDateTime.now();

        assertThrows(ResponseStatusException.class, () ->
                collaboratorService.getSoldItemsReport(10L, null, end, companyId));
    }

    @Test
    @DisplayName("Deve falhar ao buscar relatório com data final nula")
    void shouldFailReportWhenEndDateIsNull() {
        LocalDateTime start = LocalDateTime.now();

        assertThrows(ResponseStatusException.class, () ->
                collaboratorService.getSoldItemsReport(10L, start, null, companyId));
    }

    @Test
    @DisplayName("Deve falhar ao buscar relatório com data inicial posterior à data final")
    void shouldFailReportWhenStartDateIsAfterEndDate() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 5, 0, 0);

        assertThrows(ResponseStatusException.class, () ->
                collaboratorService.getSoldItemsReport(10L, start, end, companyId));
    }
}
