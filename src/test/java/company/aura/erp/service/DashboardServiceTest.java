package company.aura.erp.service;

import company.aura.erp.dto.dashboard.DashboardSummaryResponse;
import company.aura.erp.dto.dashboard.InstallmentPaymentSummary;
import company.aura.erp.dto.dashboard.PendingInstallmentSummary;
import company.aura.erp.dto.dashboard.OrderStatusSummary;
import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.repository.OrderInstallmentRepository;
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

    @Mock
    private OrderInstallmentRepository orderInstallmentRepository;

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
                new OrderStatusSummary(OrderStatus.PENDING_PAYMENT, 2L)
        );
        InstallmentPaymentSummary paymentSummary = new InstallmentPaymentSummary(
                new BigDecimal("10000.00"),
                new BigDecimal("3500.00")
        );
        PendingInstallmentSummary pendingInstallments = new PendingInstallmentSummary(3L, new BigDecimal("1500.00"));

        when(orderRepository.summarizeByStatusesAndDateRange(
                eq(companyId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(salesPerStatus);

        when(orderInstallmentRepository.getPaymentsSummaryByOrderDate(
                eq(companyId),
                eq(List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.COMPLETED)),
                eq(List.of(InstallmentStatus.CANCELED)),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(paymentSummary);

        when(orderInstallmentRepository.getPendingInstallmentsSummaryByDueDate(
                eq(companyId),
                eq(startDate),
                eq(endDate)
        )).thenReturn(pendingInstallments);

        DashboardSummaryResponse response = dashboardService.getSummary(startDate, endDate, companyId);

        assertNotNull(response);
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
        assertEquals(salesPerStatus, response.salesPerStatus());
        assertEquals(new BigDecimal("10000.00"), response.paidAmount());
        assertEquals(new BigDecimal("3500.00"), response.pendingAmount());
        assertEquals(new BigDecimal("13500.00"), response.totalAmount());
        assertEquals(pendingInstallments, response.pendingInstallments());
    }

    @Test
    @DisplayName("getSummary — Deve aplicar valores padrão do mês atual quando datas forem nulas")
    void shouldApplyDefaultsWhenDatesAreNull() {
        Long companyId = 1L;

        when(orderRepository.summarizeByStatusesAndDateRange(
                eq(companyId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());

        when(orderInstallmentRepository.getPaymentsSummaryByOrderDate(
                eq(companyId),
                eq(List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.COMPLETED)),
                eq(List.of(InstallmentStatus.CANCELED)),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(new InstallmentPaymentSummary(BigDecimal.ZERO, BigDecimal.ZERO));

        when(orderInstallmentRepository.getPendingInstallmentsSummaryByDueDate(
                eq(companyId),
                eq(LocalDate.now().withDayOfMonth(1)),
                eq(LocalDate.now())
        )).thenReturn(new PendingInstallmentSummary(0L, BigDecimal.ZERO));

        DashboardSummaryResponse response = dashboardService.getSummary(null, null, companyId);

        assertNotNull(response);
        assertEquals(LocalDate.now().withDayOfMonth(1), response.startDate());
        assertEquals(LocalDate.now(), response.endDate());
        assertEquals(Collections.emptyList(), response.salesPerStatus());
        assertEquals(BigDecimal.ZERO, response.paidAmount());
        assertEquals(BigDecimal.ZERO, response.pendingAmount());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
        assertEquals(new PendingInstallmentSummary(0L, BigDecimal.ZERO), response.pendingInstallments());
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
                orderInstallmentRepository,
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
