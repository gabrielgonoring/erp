package company.aura.erp.controller;

import company.aura.erp.dto.dashboard.DashboardKpisResponse;
import company.aura.erp.dto.dashboard.DashboardSummaryResponse;
import company.aura.erp.dto.dashboard.PendingReceivableSummary;
import company.aura.erp.dto.dashboard.OrderStatusSummary;
import company.aura.erp.dto.dashboard.ServiceOrderStatusSummary;
import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.ServiceOrderStatus;
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
        PendingReceivableSummary pendingReceivables = new PendingReceivableSummary(2L, new BigDecimal("1000.00"));
        DashboardSummaryResponse expectedResponse = new DashboardSummaryResponse(
                startDate,
                endDate,
                List.of(new OrderStatusSummary(OrderStatus.COMPLETED, 2L)),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("6000.00"),
                pendingReceivables,
                null
        );

        when(dashboardService.getSummary(startDate, endDate, 10L)).thenReturn(expectedResponse);

        ResponseEntity<DashboardSummaryResponse> response = dashboardController.getSummary(startDate, endDate, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        verify(dashboardService).getSummary(startDate, endDate, 10L);
    }

    @Test
    @DisplayName("GET /api/dashboard/kpis — Deve retornar KPIs do dashboard com sucesso via GET")
    void shouldReturnKpisOnGet() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 23);
        DashboardKpisResponse expectedResponse = new DashboardKpisResponse(
                new DashboardKpisResponse.Period(startDate, endDate),
                new DashboardKpisResponse.Kpis(
                        new DashboardKpisResponse.RealizedRevenue(
                                new BigDecimal("8172.66"),
                                new BigDecimal("7297.00"),
                                new BigDecimal("12.0"),
                                26L
                        ),
                        new DashboardKpisResponse.GeneratedSales(
                                new BigDecimal("12432.34"),
                                new BigDecimal("10096.50"),
                                17L,
                                new BigDecimal("2335.84"),
                                5L
                        ),
                        new DashboardKpisResponse.OpenReceivables(
                                new BigDecimal("2335.84"),
                                8L,
                                new BigDecimal("1250.00"),
                                3L
                        ),
                        new DashboardKpisResponse.OverdueReceivables(
                                new BigDecimal("1540.00"),
                                4L,
                                3L
                        )
                )
        );

        when(dashboardService.getKpis(startDate, endDate, 10L)).thenReturn(expectedResponse);

        ResponseEntity<DashboardKpisResponse> response = dashboardController.getKpis(startDate, endDate, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        verify(dashboardService).getKpis(startDate, endDate, 10L);
    }

    @Test
    @DisplayName("GET /api/dashboard/orders/status-summary — Deve retornar resumo dos status dos pedidos")
    void shouldReturnOrdersStatusSummaryOnGet() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 23);
        List<OrderStatusSummary> expectedResponse = List.of(
                new OrderStatusSummary(OrderStatus.CANCELED, 1L),
                new OrderStatusSummary(OrderStatus.COMPLETED, 5L)
        );

        when(dashboardService.getOrdersStatusSummary(startDate, endDate, 10L)).thenReturn(expectedResponse);

        ResponseEntity<List<OrderStatusSummary>> response = dashboardController.getOrdersStatusSummary(startDate, endDate, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        verify(dashboardService).getOrdersStatusSummary(startDate, endDate, 10L);
    }

    @Test
    @DisplayName("GET /api/dashboard/service-orders/status-summary — Deve retornar resumo dos status das OSs")
    void shouldReturnServiceOrdersStatusSummaryOnGet() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 23);
        List<ServiceOrderStatusSummary> expectedResponse = List.of(
                new ServiceOrderStatusSummary(ServiceOrderStatus.CANCELED, 1L),
                new ServiceOrderStatusSummary(ServiceOrderStatus.IN_PROGRESS, 3L)
        );

        when(dashboardService.getServiceOrdersStatusSummary(startDate, endDate, 10L)).thenReturn(expectedResponse);

        ResponseEntity<List<ServiceOrderStatusSummary>> response = dashboardController.getServiceOrdersStatusSummary(startDate, endDate, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        verify(dashboardService).getServiceOrdersStatusSummary(startDate, endDate, 10L);
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
