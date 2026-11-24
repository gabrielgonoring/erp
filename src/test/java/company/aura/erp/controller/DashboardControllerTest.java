package company.aura.erp.controller;

import company.aura.erp.dto.dashboard.DashboardKpisResponse;
import company.aura.erp.dto.dashboard.OrderStatusTotalSummary;
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
    @DisplayName("GET /api/dashboard/orders/status-summary — Deve retornar resumo dos status e valores totais dos pedidos")
    void shouldReturnOrdersStatusSummaryOnGet() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 23);
        List<OrderStatusTotalSummary> expectedResponse = List.of(
                new OrderStatusTotalSummary(OrderStatus.CANCELED, 1L, new BigDecimal("150.00")),
                new OrderStatusTotalSummary(OrderStatus.COMPLETED, 5L, new BigDecimal("1250.50"))
        );

        when(dashboardService.getOrdersStatusSummary(startDate, endDate, 10L)).thenReturn(expectedResponse);

        ResponseEntity<List<OrderStatusTotalSummary>> response = dashboardController.getOrdersStatusSummary(startDate, endDate, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        verify(dashboardService).getOrdersStatusSummary(startDate, endDate, 10L);
    }

    @Test
    @DisplayName("GET /api/dashboard/service-orders/status-summary — Deve retornar resumo dos status e valores totais das OSs")
    void shouldReturnServiceOrdersStatusSummaryOnGet() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 23);
        List<ServiceOrderStatusSummary> expectedResponse = List.of(
                new ServiceOrderStatusSummary(ServiceOrderStatus.CANCELED, 1L, new BigDecimal("200.00")),
                new ServiceOrderStatusSummary(ServiceOrderStatus.IN_PROGRESS, 3L, new BigDecimal("980.00"))
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
