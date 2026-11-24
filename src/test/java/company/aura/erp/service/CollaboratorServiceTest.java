package company.aura.erp.service;

import company.aura.erp.dto.collaborator.*;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.CommissionEntryEntity;
import company.aura.erp.entity.CommissionRuleEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.enums.*;
import company.aura.erp.repository.*;
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
import java.time.LocalDate;
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

    @Mock
    private CommissionRuleRepository commissionRuleRepository;

    @Mock
    private CommissionEntryRepository commissionEntryRepository;

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

    @Test
    @DisplayName("Deve gerar relatório de itens vendidos usando regras e lançamentos de comissão")
    void shouldGetSoldItemsReportUsingCommissionRulesAndEntries() {
        OrderItemEntity item1 = OrderItemEntity.builder()
                .id(101L)
                .orderId(50L)
                .description("Corte")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("100.00"))
                .totalPrice(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.of(2026, 11, 10, 10, 0))
                .build();

        CommissionRuleEntity collabDefaultRule = CommissionRuleEntity.builder()
                .id(1L)
                .companyId(companyId)
                .collaboratorId(10L)
                .commissionPercentage(new BigDecimal("10.00"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();

        CommissionEntryEntity entry = CommissionEntryEntity.builder()
                .id(500L)
                .companyId(companyId)
                .sourceType(CommissionSourceType.ORDER_ITEM)
                .sourceItemId(101L)
                .commissionAmount(new BigDecimal("15.00"))
                .status(CommissionEntryStatus.PENDING)
                .build();

        when(collaboratorRepository.findByIdAndCompanyId(10L, companyId)).thenReturn(Optional.of(collaborator));
        when(orderItemRepository.findByCollaboratorAndCompanyAndCreatedAtBetween(
                eq(10L), eq(companyId), eq(List.of(OrderStatus.COMPLETED)), any(), any()))
                .thenReturn(List.of(item1));
        when(commissionRuleRepository.findByCompanyIdAndStatus(companyId, CommissionRuleStatus.ACTIVE))
                .thenReturn(List.of(collabDefaultRule));
        when(commissionEntryRepository.findByCompanyIdAndSourceTypeAndSourceItemIdInAndStatusNot(
                companyId, CommissionSourceType.ORDER_ITEM, List.of(101L), CommissionEntryStatus.CANCELED))
                .thenReturn(List.of(entry));

        CollaboratorCommissionReportResponse report = collaboratorService.getSoldItemsReport(
                10L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), companyId
        );

        assertEquals(new BigDecimal("100.00"), report.getHeader().getTotalSold());
        assertEquals(new BigDecimal("15.00"), report.getHeader().getTotalCommission());
        assertEquals(new BigDecimal("10.00"), report.getHeader().getCommissionPercentage());
        assertEquals(1, report.getItems().size());
        assertEquals(new BigDecimal("15.00"), report.getItems().getFirst().getCommissionAmount());
    }
}
