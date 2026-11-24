package company.aura.erp.service;

import company.aura.erp.dto.dashboard.DashboardKpisResponse;
import company.aura.erp.dto.dashboard.DashboardSummaryResponse;
import company.aura.erp.dto.dashboard.OrderStatusSummary;
import company.aura.erp.dto.dashboard.PaymentTransactionSummary;
import company.aura.erp.dto.dashboard.PendingReceivableSummary;
import company.aura.erp.dto.dashboard.RealizedRevenueSummary;
import company.aura.erp.dto.dashboard.ReceivablePaymentSummary;
import company.aura.erp.dto.dashboard.ReceivablesKpiSummary;
import company.aura.erp.dto.dashboard.SalesAggregationSummary;
import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.enums.ServiceOrderStatus;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
import company.aura.erp.repository.ReceivableRepository;
import company.aura.erp.repository.ServiceOrderRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

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

        List<OrderStatusSummary> salesPerStatus = List.of(
                new OrderStatusSummary(OrderStatus.COMPLETED, 4L),
                new OrderStatusSummary(OrderStatus.OPEN, 2L)
        );
        ReceivablePaymentSummary paymentSummary = new ReceivablePaymentSummary(
                new BigDecimal("10000.00"),
                new BigDecimal("3500.00")
        );
        PendingReceivableSummary pendingReceivables = new PendingReceivableSummary(3L, new BigDecimal("1500.00"));
        PaymentTransactionSummary revenue = new PaymentTransactionSummary(5L, new BigDecimal("8000.00"));

        when(orderRepository.summarizeByStatusesAndDateRange(
                eq(companyId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(salesPerStatus);

        when(receivableRepository.getPaymentsSummaryByOrderDate(
                eq(companyId),
                eq(List.of(OrderStatus.COMPLETED)),
                eq(List.of(ReceivableStatus.CANCELED)),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(paymentSummary);

        when(receivableRepository.getPendingReceivablesSummaryByDueDate(
                eq(companyId),
                eq(startDate),
                eq(endDate)
        )).thenReturn(pendingReceivables);

        when(paymentTransactionRepository.getPaymentsSummaryByStatusInAndDateRange(
                eq(companyId),
                eq(List.of(ReceivableStatus.PAID, ReceivableStatus.PARTIALLY_PAID)),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(revenue);

        DashboardSummaryResponse response = dashboardService.getSummary(startDate, endDate, companyId);

        assertNotNull(response);
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
        assertEquals(salesPerStatus, response.salesPerStatus());
        assertEquals(new BigDecimal("10000.00"), response.paidAmount());
        assertEquals(new BigDecimal("3500.00"), response.pendingAmount());
        assertEquals(new BigDecimal("13500.00"), response.totalAmount());
        assertEquals(pendingReceivables, response.pendingReceivables());
        assertEquals(revenue, response.revenue());
    }

    @Test
    @DisplayName("getSummary — Deve aplicar valores padrão do mês atual quando datas forem nulas")
    void shouldApplyDefaultsWhenDatesAreNull() {
        Long companyId = 1L;
        PaymentTransactionSummary revenue = new PaymentTransactionSummary(0L, BigDecimal.ZERO);

        when(orderRepository.summarizeByStatusesAndDateRange(
                eq(companyId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());

        when(receivableRepository.getPaymentsSummaryByOrderDate(
                eq(companyId),
                eq(List.of(OrderStatus.COMPLETED)),
                eq(List.of(ReceivableStatus.CANCELED)),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new ReceivablePaymentSummary(BigDecimal.ZERO, BigDecimal.ZERO));

        when(receivableRepository.getPendingReceivablesSummaryByDueDate(
                eq(companyId),
                eq(LocalDate.now().withDayOfMonth(1)),
                eq(LocalDate.now())
        )).thenReturn(new PendingReceivableSummary(0L, BigDecimal.ZERO));

        when(paymentTransactionRepository.getPaymentsSummaryByStatusInAndDateRange(
                eq(companyId),
                eq(List.of(ReceivableStatus.PAID, ReceivableStatus.PARTIALLY_PAID)),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(revenue);

        DashboardSummaryResponse response = dashboardService.getSummary(null, null, companyId);

        assertNotNull(response);
        assertEquals(LocalDate.now().withDayOfMonth(1), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
        assertEquals(Collections.emptyList(), response.salesPerStatus());
        assertEquals(BigDecimal.ZERO, response.paidAmount());
        assertEquals(BigDecimal.ZERO, response.pendingAmount());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
        assertEquals(new PendingReceivableSummary(0L, BigDecimal.ZERO), response.pendingReceivables());
        assertEquals(revenue, response.revenue());
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
    @DisplayName("getKpis — Deve calcular todos os KPIs usando executorService")
    void shouldCalculateKpisCorrectly() {
        Long companyId = 1L;
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 23);
        LocalDateTime expectedStartDateTime = startDate.atStartOfDay();
        LocalDateTime expectedEndDateTime = endDate.atTime(LocalTime.MAX);
        LocalDateTime expectedPrevStartDateTime = LocalDate.of(2026, 10, 9).atStartOfDay();

        when(paymentTransactionRepository.getRealizedRevenueSummary(
                eq(companyId),
                eq(expectedStartDateTime),
                eq(expectedEndDateTime),
                eq(expectedPrevStartDateTime)
        )).thenReturn(new RealizedRevenueSummary(
                new BigDecimal("8172.66"),
                26L,
                new BigDecimal("7297.00")
        ));

        when(orderRepository.getStandaloneOrdersSalesSummary(
                eq(companyId),
                eq(OrderStatus.CANCELED),
                eq(expectedStartDateTime),
                eq(expectedEndDateTime)
        )).thenReturn(new SalesAggregationSummary(
                new BigDecimal("10096.50"),
                17L
        ));

        when(serviceOrderRepository.getServiceOrdersSalesSummary(
                eq(companyId),
                eq(ServiceOrderStatus.CANCELED),
                eq(expectedStartDateTime),
                eq(expectedEndDateTime)
        )).thenReturn(new SalesAggregationSummary(
                new BigDecimal("2335.84"),
                5L
        ));

        when(receivableRepository.getReceivablesKpiSummary(
                eq(companyId),
                eq(LocalDate.now()),
                eq(LocalDate.now().plusDays(7)),
                eq(List.of(ReceivableStatus.PENDING, ReceivableStatus.PARTIALLY_PAID, ReceivableStatus.OVERDUE))
        )).thenReturn(new ReceivablesKpiSummary(
                new BigDecimal("2335.84"),
                8L,
                new BigDecimal("1250.00"),
                3L,
                new BigDecimal("1540.00"),
                4L,
                3L
        ));

        DashboardKpisResponse response = dashboardService.getKpis(startDate, endDate, companyId);

        assertNotNull(response);
        assertEquals(startDate, response.period().startDate());
        assertEquals(endDate, response.period().endDate());

        assertEquals(new BigDecimal("8172.66"), response.kpis().realizedRevenue().amount());
        assertEquals(new BigDecimal("7297.00"), response.kpis().realizedRevenue().previousAmount());
        assertEquals(new BigDecimal("12.0"), response.kpis().realizedRevenue().growthPercentage());
        assertEquals(26L, response.kpis().realizedRevenue().paymentsCount());

        assertEquals(new BigDecimal("12432.34"), response.kpis().generatedSales().totalAmount());
        assertEquals(new BigDecimal("10096.50"), response.kpis().generatedSales().ordersAmount());
        assertEquals(17L, response.kpis().generatedSales().ordersCount());
        assertEquals(new BigDecimal("2335.84"), response.kpis().generatedSales().serviceOrdersAmount());
        assertEquals(5L, response.kpis().generatedSales().serviceOrdersCount());

        assertEquals(new BigDecimal("2335.84"), response.kpis().openReceivables().totalAmount());
        assertEquals(8L, response.kpis().openReceivables().installmentsCount());
        assertEquals(new BigDecimal("1250.00"), response.kpis().openReceivables().dueThisWeekAmount());
        assertEquals(3L, response.kpis().openReceivables().dueThisWeekCount());

        assertEquals(new BigDecimal("1540.00"), response.kpis().overdueReceivables().totalAmount());
        assertEquals(4L, response.kpis().overdueReceivables().installmentsCount());
        assertEquals(3L, response.kpis().overdueReceivables().customersCount());

        verify(executorService, atLeast(4)).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("getKpis — Deve aplicar datas padrão e calcular crescimento quando período anterior for zero")
    void shouldApplyDefaultsAndHandleZeroPreviousRevenueInGetKpis() {
        Long companyId = 1L;
        LocalDate today = LocalDate.now();
        LocalDate expectedStart = today.withDayOfMonth(1);

        when(paymentTransactionRepository.getRealizedRevenueSummary(
                eq(companyId),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new RealizedRevenueSummary(new BigDecimal("500.00"), 2L, BigDecimal.ZERO));

        when(orderRepository.getStandaloneOrdersSalesSummary(
                eq(companyId),
                eq(OrderStatus.CANCELED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new SalesAggregationSummary(BigDecimal.ZERO, 0L));

        when(serviceOrderRepository.getServiceOrdersSalesSummary(
                eq(companyId),
                eq(ServiceOrderStatus.CANCELED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new SalesAggregationSummary(BigDecimal.ZERO, 0L));

        when(receivableRepository.getReceivablesKpiSummary(
                eq(companyId),
                eq(today),
                eq(today.plusDays(7)),
                anyList()
        )).thenReturn(new ReceivablesKpiSummary(
                BigDecimal.ZERO, 0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO, 0L, 0L
        ));

        DashboardKpisResponse response = dashboardService.getKpis(null, null, companyId);

        assertEquals(expectedStart, response.period().startDate());
        assertEquals(today, response.period().endDate());
        assertEquals(new BigDecimal("100.0"), response.kpis().realizedRevenue().growthPercentage());
        assertEquals(new BigDecimal("0.00"), response.kpis().generatedSales().totalAmount());
    }

    @Test
    @DisplayName("getKpis — Deve lançar exceção se data inicial for posterior à final")
    void shouldThrowExceptionInGetKpisWhenStartDateIsAfterEndDate() {
        Long companyId = 1L;
        LocalDate startDate = LocalDate.of(2026, 11, 10);
        LocalDate endDate = LocalDate.of(2026, 11, 5);

        assertThrows(ResponseStatusException.class, () -> dashboardService.getKpis(startDate, endDate, companyId));
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
                serviceOrderRepository,
                receivableRepository,
                paymentTransactionRepository,
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
