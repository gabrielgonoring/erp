package company.aura.erp.controller;

import company.aura.erp.dto.commission.CommissionRuleCreateRequest;
import company.aura.erp.dto.commission.CommissionRuleResponse;
import company.aura.erp.dto.commission.CommissionRuleSummaryResponse;
import company.aura.erp.dto.commission.CommissionRuleUpdateRequest;
import company.aura.erp.enums.CommissionRuleStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CommissionRuleService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionRuleControllerTest {

    @Mock
    private CommissionRuleService commissionRuleService;

    @InjectMocks
    private CommissionRuleController commissionRuleController;

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
    @DisplayName("POST /api/commission-rules — Deve cadastrar regra de comissão e retornar HTTP 201")
    void shouldCreateRule() {
        CommissionRuleCreateRequest request = CommissionRuleCreateRequest.builder()
                .collaboratorId(5L)
                .categoryId(2L)
                .commissionPercentage(new BigDecimal("10.00"))
                .validFrom(LocalDate.of(2026, 1, 1))
                .build();

        CommissionRuleResponse expected = CommissionRuleResponse.builder()
                .id(1L)
                .companyId(10L)
                .collaboratorId(5L)
                .collaboratorName("João")
                .categoryId(2L)
                .categoryName("Serviços")
                .commissionPercentage(new BigDecimal("10.00"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();

        when(commissionRuleService.create(request, 10L)).thenReturn(expected);

        ResponseEntity<CommissionRuleResponse> response = commissionRuleController.create(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionRuleService).create(request, 10L);
    }

    @Test
    @DisplayName("PUT /api/commission-rules/{id} — Deve editar regra de comissão e retornar HTTP 200")
    void shouldUpdateRule() {
        CommissionRuleUpdateRequest request = CommissionRuleUpdateRequest.builder()
                .collaboratorId(5L)
                .catalogItemId(8L)
                .commissionPercentage(new BigDecimal("12.50"))
                .build();

        CommissionRuleResponse expected = CommissionRuleResponse.builder()
                .id(1L)
                .companyId(10L)
                .collaboratorId(5L)
                .catalogItemId(8L)
                .commissionPercentage(new BigDecimal("12.50"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();

        when(commissionRuleService.update(1L, request, 10L)).thenReturn(expected);

        ResponseEntity<CommissionRuleResponse> response = commissionRuleController.update(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionRuleService).update(1L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/commission-rules/{id} — Deve deletar regra de comissão e retornar HTTP 204")
    void shouldDeleteRule() {
        ResponseEntity<Void> response = commissionRuleController.delete(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(commissionRuleService).delete(1L, 10L);
    }

    @Test
    @DisplayName("GET /api/commission-rules/{id} — Deve buscar regra de comissão por ID")
    void shouldFindRuleById() {
        CommissionRuleResponse expected = CommissionRuleResponse.builder()
                .id(1L)
                .companyId(10L)
                .commissionPercentage(new BigDecimal("8.00"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();

        when(commissionRuleService.findById(1L, 10L)).thenReturn(expected);

        ResponseEntity<CommissionRuleResponse> response = commissionRuleController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(commissionRuleService).findById(1L, 10L);
    }

    @Test
    @DisplayName("GET /api/commission-rules — Deve listar regras de comissão de forma paginada")
    void shouldListRules() {
        Pageable pageable = PageRequest.of(0, 20);
        CommissionRuleSummaryResponse summary = CommissionRuleSummaryResponse.builder()
                .id(1L)
                .collaboratorId(5L)
                .collaboratorName("João")
                .commissionPercentage(new BigDecimal("10.00"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();
        Page<CommissionRuleSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(commissionRuleService.listSummarized("João", 5L, null, null, CommissionRuleStatus.ACTIVE, 10L, pageable))
                .thenReturn(page);

        ResponseEntity<Page<CommissionRuleSummaryResponse>> response = commissionRuleController.list(
                "João", 5L, null, null, CommissionRuleStatus.ACTIVE, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(commissionRuleService).listSummarized("João", 5L, null, null, CommissionRuleStatus.ACTIVE, 10L, pageable);
    }
}
