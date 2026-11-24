package company.aura.erp.controller;

import company.aura.erp.dto.commission.*;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.enums.CommissionEntryStatus;
import company.aura.erp.enums.CommissionSourceType;
import company.aura.erp.enums.CommissionStatementStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CommissionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionControllerTest {

    @Mock
    private CommissionService commissionService;

    @InjectMocks
    private CommissionController commissionController;

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
    @DisplayName("POST /api/commissions/entries/generate — Deve gerar lançamentos de comissão e retornar HTTP 201")
    void shouldGenerateEntries() {
        CommissionGenerateEntriesRequest request = CommissionGenerateEntriesRequest.builder()
                .orderId(100L)
                .build();
        List<CommissionEntryResponse> expected = List.of(
                CommissionEntryResponse.builder().id(1L).commissionAmount(new BigDecimal("25.00")).build()
        );

        when(commissionService.generateEntries(request, 10L)).thenReturn(expected);

        ResponseEntity<List<CommissionEntryResponse>> response = commissionController.generateEntries(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionService).generateEntries(request, 10L);
    }

    @Test
    @DisplayName("GET /api/commissions/entries — Deve listar lançamentos de comissão paginados")
    void shouldListEntries() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CommissionEntryResponse> page = new PageImpl<>(
                List.of(CommissionEntryResponse.builder().id(1L).build()), pageable, 1
        );

        when(commissionService.listEntries(null, 5L, null, List.of(CommissionEntryStatus.PENDING),
                CommissionSourceType.ORDER_ITEM, null, null, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<CommissionEntryResponse>> response = commissionController.listEntries(
                null, 5L, null, List.of(CommissionEntryStatus.PENDING),
                CommissionSourceType.ORDER_ITEM, null, null, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(commissionService).listEntries(null, 5L, null, List.of(CommissionEntryStatus.PENDING),
                CommissionSourceType.ORDER_ITEM, null, null, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/commissions/entries/{entryId} — Deve buscar lançamento de comissão por ID")
    void shouldFindEntryById() {
        CommissionEntryResponse expected = CommissionEntryResponse.builder().id(1L).build();
        when(commissionService.findEntryById(1L, 10L)).thenReturn(expected);

        ResponseEntity<CommissionEntryResponse> response = commissionController.findEntryById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionService).findEntryById(1L, 10L);
    }

    @Test
    @DisplayName("DELETE /api/commissions/entries/{entryId} — Deve cancelar lançamento e retornar HTTP 204")
    void shouldCancelEntry() {
        ResponseEntity<Void> response = commissionController.cancelEntry(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(commissionService).cancelEntry(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/commissions/statements — Deve criar fechamento de comissão e retornar HTTP 201")
    void shouldCreateStatement() {
        CommissionStatementCreateRequest request = CommissionStatementCreateRequest.builder()
                .collaboratorId(5L)
                .periodStart(LocalDate.of(2026, 11, 1))
                .periodEnd(LocalDate.of(2026, 11, 30))
                .dueDate(LocalDate.of(2026, 12, 5))
                .build();
        CommissionStatementResponse expected = CommissionStatementResponse.builder()
                .id(10L)
                .status(CommissionStatementStatus.OPEN)
                .totalCommissionAmount(new BigDecimal("150.00"))
                .build();

        when(commissionService.createStatement(request, 10L)).thenReturn(expected);

        ResponseEntity<CommissionStatementResponse> response = commissionController.createStatement(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionService).createStatement(request, 10L);
    }

    @Test
    @DisplayName("GET /api/commissions/statements — Deve listar fechamentos de comissão paginados")
    void shouldListStatements() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CommissionStatementSummaryResponse> page = new PageImpl<>(
                List.of(CommissionStatementSummaryResponse.builder().id(10L).build()), pageable, 1
        );

        when(commissionService.listStatements(null, 5L, null, null, null, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<CommissionStatementSummaryResponse>> response = commissionController.listStatements(
                null, 5L, null, null, null, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(commissionService).listStatements(null, 5L, null, null, null, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/commissions/statements/{statementId} — Deve buscar fechamento por ID")
    void shouldFindStatementById() {
        CommissionStatementResponse expected = CommissionStatementResponse.builder().id(10L).build();
        when(commissionService.findStatementById(10L, 10L)).thenReturn(expected);

        ResponseEntity<CommissionStatementResponse> response = commissionController.findStatementById(10L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionService).findStatementById(10L, 10L);
    }

    @Test
    @DisplayName("POST /api/commissions/statements/{statementId}/approve — Deve aprovar fechamento")
    void shouldApproveStatement() {
        CommissionStatementResponse expected = CommissionStatementResponse.builder()
                .id(10L)
                .status(CommissionStatementStatus.APPROVED)
                .payableId(99L)
                .build();
        when(commissionService.approveStatement(10L, 10L)).thenReturn(expected);

        ResponseEntity<CommissionStatementResponse> response = commissionController.approveStatement(10L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionService).approveStatement(10L, 10L);
    }

    @Test
    @DisplayName("POST /api/commissions/statements/{statementId}/pay — Deve registrar pagamento do fechamento")
    void shouldPayStatement() {
        PayableTransactionRequest request = new PayableTransactionRequest(
                new BigDecimal("150.00"), PaymentMethod.PIX, null, null, "Pgto comissão"
        );
        CommissionStatementResponse expected = CommissionStatementResponse.builder()
                .id(10L)
                .status(CommissionStatementStatus.PAID)
                .build();

        when(commissionService.payStatement(10L, request, 10L)).thenReturn(expected);

        ResponseEntity<CommissionStatementResponse> response = commissionController.payStatement(10L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionService).payStatement(10L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/commissions/statements/{statementId} — Deve cancelar fechamento e retornar HTTP 204")
    void shouldCancelStatement() {
        ResponseEntity<Void> response = commissionController.cancelStatement(10L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(commissionService).cancelStatement(10L, 10L);
    }
}
