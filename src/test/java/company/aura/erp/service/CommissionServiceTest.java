package company.aura.erp.service;

import company.aura.erp.dto.commission.*;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.entity.*;
import company.aura.erp.enums.*;
import company.aura.erp.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock
    private CommissionRuleRepository commissionRuleRepository;
    @Mock
    private CommissionStatementRepository commissionStatementRepository;
    @Mock
    private CommissionEntryRepository commissionEntryRepository;
    @Mock
    private CollaboratorRepository collaboratorRepository;
    @Mock
    private CatalogItemRepository catalogItemRepository;
    @Mock
    private PayableRepository payableRepository;
    @Mock
    private PayableService payableService;

    @InjectMocks
    private CommissionService commissionService;

    @Test
    @DisplayName("Deve gerar lançamento aplicando hierarquia de regras (colaborador + categoria vence regra geral)")
    void shouldGenerateEntriesUsingSpecificityHierarchy() {
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(10L)
                .status(OrderStatus.COMPLETED)
                .completedAt(LocalDateTime.of(2026, 11, 20, 14, 0))
                .build();
        OrderItemEntity item = OrderItemEntity.builder()
                .id(500L)
                .orderId(100L)
                .catalogItemId(77L)
                .description("Corte Premium")
                .totalPrice(new BigDecimal("200.00"))
                .professionalId(5L)
                .build();

        CollaboratorEntity collaborator = CollaboratorEntity.builder()
                .id(5L)
                .companyId(10L)
                .name("João")
                .build();

        CatalogItemEntity catalogItem = new CatalogItemEntity();
        catalogItem.setId(77L);
        catalogItem.setCompanyId(10L);
        catalogItem.setCategoryId(3L);

        CommissionRuleEntity companyDefaultRule = CommissionRuleEntity.builder()
                .id(1L)
                .companyId(10L)
                .commissionPercentage(new BigDecimal("8.00"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();
        CommissionRuleEntity collabCategoryRule = CommissionRuleEntity.builder()
                .id(2L)
                .companyId(10L)
                .collaboratorId(5L)
                .categoryId(3L)
                .commissionPercentage(new BigDecimal("15.00"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();

        when(commissionRuleRepository.findByCompanyIdAndStatus(10L, CommissionRuleStatus.ACTIVE))
                .thenReturn(List.of(companyDefaultRule, collabCategoryRule));
        when(commissionEntryRepository.existsByCompanyIdAndSourceTypeAndSourceItemId(10L, CommissionSourceType.ORDER_ITEM, 500L))
                .thenReturn(false);
        when(collaboratorRepository.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.of(collaborator));
        when(catalogItemRepository.findByIdAndCompanyId(77L, 10L)).thenReturn(Optional.of(catalogItem));
        when(commissionEntryRepository.save(any(CommissionEntryEntity.class))).thenAnswer(inv -> {
            CommissionEntryEntity e = inv.getArgument(0);
            e.setId(900L);
            return e;
        });

        List<CommissionEntryResponse> result = commissionService.generateEntriesForOrder(order, List.of(item));

        assertEquals(1, result.size());
        CommissionEntryResponse entry = result.get(0);
        assertEquals(new BigDecimal("15.00"), entry.getAppliedPercentage());
        assertEquals(new BigDecimal("30.00"), entry.getCommissionAmount());
        assertEquals(CommissionRuleSource.COLLABORATOR_CATEGORY, entry.getRuleSource());
        assertEquals(2L, entry.getRuleId());
        assertEquals(3L, entry.getCategoryId());
    }

    @Test
    @DisplayName("Não deve gerar lançamento quando nenhuma regra de comissão estiver ativa")
    void shouldNotGenerateEntryWhenNoRuleApplies() {
        OrderEntity order = OrderEntity.builder()
                .id(200L)
                .companyId(10L)
                .status(OrderStatus.COMPLETED)
                .completedAt(LocalDateTime.of(2026, 11, 20, 16, 0))
                .build();
        OrderItemEntity item = OrderItemEntity.builder()
                .id(600L)
                .orderId(200L)
                .description("Reparo")
                .totalPrice(new BigDecimal("100.00"))
                .professionalId(5L)
                .build();

        CollaboratorEntity collaborator = CollaboratorEntity.builder()
                .id(5L)
                .companyId(10L)
                .name("João")
                .build();

        when(commissionRuleRepository.findByCompanyIdAndStatus(10L, CommissionRuleStatus.ACTIVE))
                .thenReturn(List.of());
        when(commissionEntryRepository.existsByCompanyIdAndSourceTypeAndSourceItemId(10L, CommissionSourceType.ORDER_ITEM, 600L))
                .thenReturn(false);
        when(collaboratorRepository.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.of(collaborator));

        List<CommissionEntryResponse> result = commissionService.generateEntriesForOrder(order, List.of(item));

        assertTrue(result.isEmpty());
        verify(commissionEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar fechamento, aprovar gerando conta a pagar e permitir pagamento")
    void shouldCreateApproveAndPayStatement() {
        CollaboratorEntity collaborator = CollaboratorEntity.builder()
                .id(5L)
                .companyId(10L)
                .name("João")
                .build();
        CommissionEntryEntity entry = CommissionEntryEntity.builder()
                .id(900L)
                .companyId(10L)
                .collaboratorId(5L)
                .collaboratorName("João")
                .baseAmount(new BigDecimal("200.00"))
                .appliedPercentage(new BigDecimal("15.00"))
                .commissionAmount(new BigDecimal("30.00"))
                .status(CommissionEntryStatus.PENDING)
                .occurredAt(LocalDateTime.of(2026, 11, 20, 14, 0))
                .build();

        CommissionStatementCreateRequest createRequest = CommissionStatementCreateRequest.builder()
                .collaboratorId(5L)
                .periodStart(LocalDate.of(2026, 11, 1))
                .periodEnd(LocalDate.of(2026, 11, 30))
                .dueDate(LocalDate.of(2026, 12, 5))
                .build();

        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(5L, 10L, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));
        when(commissionEntryRepository.findByCompanyIdAndCollaboratorIdAndStatusAndStatementIdIsNullAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(
                eq(10L), eq(5L), eq(CommissionEntryStatus.PENDING), any(), any()))
                .thenReturn(List.of(entry));
        when(commissionStatementRepository.save(any(CommissionStatementEntity.class))).thenAnswer(inv -> {
            CommissionStatementEntity st = inv.getArgument(0);
            if (st.getId() == null) {
                st.setId(50L);
            }
            return st;
        });
        when(commissionEntryRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        CommissionStatementResponse created = commissionService.createStatement(createRequest, 10L);

        assertEquals(50L, created.getId());
        assertEquals(CommissionStatementStatus.OPEN, created.getStatus());
        assertEquals(new BigDecimal("30.00"), created.getTotalCommissionAmount());
        assertEquals(CommissionEntryStatus.IN_STATEMENT, entry.getStatus());

        // Approve
        CommissionStatementEntity savedStatement = CommissionStatementEntity.builder()
                .id(50L)
                .companyId(10L)
                .collaboratorId(5L)
                .collaboratorName("João")
                .periodStart(LocalDate.of(2026, 11, 1))
                .periodEnd(LocalDate.of(2026, 11, 30))
                .dueDate(LocalDate.of(2026, 12, 5))
                .totalSalesAmount(new BigDecimal("200.00"))
                .totalCommissionAmount(new BigDecimal("30.00"))
                .status(CommissionStatementStatus.OPEN)
                .build();

        when(commissionStatementRepository.findByIdAndCompanyId(50L, 10L)).thenReturn(Optional.of(savedStatement));
        when(payableRepository.save(any(PayableEntity.class))).thenAnswer(inv -> {
            PayableEntity p = inv.getArgument(0);
            p.setId(777L);
            return p;
        });
        when(commissionEntryRepository.findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(50L, 10L))
                .thenReturn(List.of(entry));

        CommissionStatementResponse approved = commissionService.approveStatement(50L, 10L);

        assertEquals(CommissionStatementStatus.APPROVED, approved.getStatus());
        assertEquals(777L, approved.getPayableId());
        assertEquals(CommissionEntryStatus.APPROVED, entry.getStatus());

        // Pay
        PayableTransactionRequest payRequest = new PayableTransactionRequest(
                new BigDecimal("30.00"), PaymentMethod.PIX, null, null, "Pgto"
        );
        commissionService.payStatement(50L, payRequest, 10L);
        verify(payableService).payPayable(10L, 777L, payRequest);
    }

    @Test
    @DisplayName("Deve cancelar fechamento e liberar lançamentos de volta para PENDING")
    void shouldCancelStatementAndReleaseEntries() {
        CommissionStatementEntity statement = CommissionStatementEntity.builder()
                .id(50L)
                .companyId(10L)
                .payableId(777L)
                .status(CommissionStatementStatus.APPROVED)
                .build();
        CommissionEntryEntity entry = CommissionEntryEntity.builder()
                .id(900L)
                .companyId(10L)
                .statementId(50L)
                .status(CommissionEntryStatus.APPROVED)
                .build();

        when(commissionStatementRepository.findByIdAndCompanyId(50L, 10L)).thenReturn(Optional.of(statement));
        when(commissionEntryRepository.findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(50L, 10L))
                .thenReturn(List.of(entry));

        commissionService.cancelStatement(50L, 10L);

        verify(payableService).cancelPayable(10L, 777L);
        assertNull(entry.getStatementId());
        assertEquals(CommissionEntryStatus.PENDING, entry.getStatus());
        assertEquals(CommissionStatementStatus.CANCELED, statement.getStatus());
    }

    @Test
    @DisplayName("Deve impedir cancelamento de lançamento que já está em um fechamento")
    void shouldRejectCancelingNonPendingEntry() {
        CommissionEntryEntity entry = CommissionEntryEntity.builder()
                .id(900L)
                .companyId(10L)
                .status(CommissionEntryStatus.IN_STATEMENT)
                .build();

        when(commissionEntryRepository.findByIdAndCompanyId(900L, 10L)).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> commissionService.cancelEntry(900L, 10L));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }
}
