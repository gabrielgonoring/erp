package company.aura.erp.service;

import company.aura.erp.dto.dashboard.DashboardSummaryResponse;
import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.alert.AlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Spy
    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Spy
    private List<AlertService> alertsValidations = new ArrayList<>();

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getSummary — Deve calcular metricas corretamente para o período selecionado")
    void shouldCalculateSummaryCorrectly() {
        Long companyId = 1L;
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);

        // Total faturamento realizado (mês)
        when(orderRepository.sumAmountByStatusAndDateRange(
                eq(companyId),
                eq(OrderStatus.COMPLETED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new BigDecimal("10000.00"));

        // Volume de vendas no período
        when(orderRepository.countByStatusAndDateRange(
                eq(companyId),
                eq(OrderStatus.COMPLETED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(4L);

        // Vendas em aberto / pendentes
        when(orderRepository.countByStatuses(
                eq(companyId),
                anyList()
        )).thenReturn(2L);

        when(orderRepository.sumAmountByStatuses(
                eq(companyId),
                anyList()
        )).thenReturn(new BigDecimal("3500.00"));

        DashboardSummaryResponse response = dashboardService.getSummary(startDate, endDate, companyId);

        assertNotNull(response);
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
        assertEquals(new BigDecimal("10000.00"), response.totalRevenue());
        assertEquals(4L, response.salesVolume().closedOrdersCount());
        assertEquals(new BigDecimal("2500.00"), response.salesVolume().averageTicket());
        assertEquals(2L, response.pendingSales().ordersCount());
        assertEquals(new BigDecimal("3500.00"), response.pendingSales().totalAmount());
    }

    @Test
    @DisplayName("getSummary — Deve aplicar valores padrão do mês atual quando datas forem nulas")
    void shouldApplyDefaultsWhenDatesAreNull() {
        Long companyId = 1L;

        when(orderRepository.sumAmountByStatusAndDateRange(
                eq(companyId),
                eq(OrderStatus.COMPLETED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(null);

        when(orderRepository.countByStatusAndDateRange(
                eq(companyId),
                eq(OrderStatus.COMPLETED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(0L);

        when(orderRepository.countByStatuses(
                eq(companyId),
                anyList()
        )).thenReturn(0L);

        when(orderRepository.sumAmountByStatuses(
                eq(companyId),
                anyList()
        )).thenReturn(null);

        DashboardSummaryResponse response = dashboardService.getSummary(null, null, companyId);

        assertNotNull(response);
        assertEquals(LocalDate.now().withDayOfMonth(1), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), response.totalRevenue());
        assertEquals(0L, response.salesVolume().closedOrdersCount());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), response.salesVolume().averageTicket());
        assertEquals(0L, response.pendingSales().ordersCount());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), response.pendingSales().totalAmount());
    }

    @Test
    @DisplayName("getSummary — Deve lançar exceção se data inicial for posterior à final")
    void shouldThrowExceptionWhenStartDateIsAfterEndDate() {
        Long companyId = 1L;
        LocalDate startDate = LocalDate.of(2026, 11, 10);
        LocalDate endDate = LocalDate.of(2026, 11, 5);

        assertThrows(ResponseStatusException.class, () -> dashboardService.getSummary(startDate, endDate, companyId));
    }

    @Test
    @DisplayName("getAlerts — Deve retornar lista vazia quando não houver alertas")
    void shouldReturnEmptyAlertsList() {
        List<SystemAlertResponse> alerts = dashboardService.getAlerts();
        assertNotNull(alerts);
        assertEquals(0, alerts.size());
    }

    @Test
    @DisplayName("getAlerts — Deve executar validações de alertas com executorService e filtrar nulos")
    void shouldReturnAlertsWhenValidationsProduceAlerts() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("admin@empresa.com")
                .name("Admin")
                .temporaryPassword(true)
                .authorities(Collections.emptyList())
                .build();

        AlertService alertService1 = mock(AlertService.class);
        AlertService alertService2 = mock(AlertService.class);

        SystemAlertResponse alertResponse = new SystemAlertResponse("0001", "Altere sua senha");

        when(alertService1.getAlert(userPrincipal)).thenReturn(alertResponse);
        when(alertService2.getAlert(userPrincipal)).thenReturn(null);

        DashboardService service = new DashboardService(
                orderRepository,
                List.of(alertService1, alertService2),
                executorService
        );

        List<SystemAlertResponse> alerts = service.getAlerts(userPrincipal);
        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        assertEquals("0001", alerts.get(0).getCode());
        assertEquals("Altere sua senha", alerts.get(0).getMessage());
    }
}
