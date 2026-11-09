package company.aura.erp.controller;

import company.aura.erp.dto.dashboard.DashboardSummaryResponse;
import company.aura.erp.dto.dashboard.DashboardSummaryResponse.PendingSalesResponse;
import company.aura.erp.dto.dashboard.DashboardSummaryResponse.SalesVolumeResponse;
import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

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
    @DisplayName("GET /api/dashboard/summary — Deve retornar resumo do dashboard com sucesso via GET")
    void shouldReturnSummaryOnGet() {
        LocalDate startDate = LocalDate.of(2026, 11, 9);
        LocalDate endDate = LocalDate.of(2026, 11, 9);
        DashboardSummaryResponse expectedResponse = new DashboardSummaryResponse(
                startDate,
                endDate,
                new BigDecimal("5000.00"),
                new SalesVolumeResponse(2L, new BigDecimal("2500.00")),
                new PendingSalesResponse(new BigDecimal("1000.00"), 1L)
        );

        when(dashboardService.getSummary(startDate, endDate, 10L)).thenReturn(expectedResponse);

        ResponseEntity<DashboardSummaryResponse> response = dashboardController.getSummary(startDate, endDate, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        verify(dashboardService).getSummary(startDate, endDate, 10L);
    }

    @Test
    @DisplayName("GET /api/dashboard/alerts — Deve retornar lista de alertas")
    void shouldReturnAlerts() {
        when(dashboardService.getAlerts(userPrincipal)).thenReturn(Collections.emptyList());

        ResponseEntity<List<SystemAlertResponse>> response = dashboardController.getAlerts(userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(dashboardService).getAlerts(userPrincipal);
    }
}
