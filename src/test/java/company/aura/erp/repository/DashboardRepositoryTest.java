package company.aura.erp.repository;

import company.aura.erp.dto.dashboard.OrderStatusTotalSummary;
import company.aura.erp.dto.dashboard.RealizedRevenueSummary;
import company.aura.erp.dto.dashboard.ReceivablesKpiSummary;
import company.aura.erp.dto.dashboard.SalesAggregationSummary;
import company.aura.erp.dto.dashboard.ServiceOrderStatusSummary;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.entity.ReceivableEntity;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.PaymentTransactionType;
import company.aura.erp.enums.ReceivableOriginType;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.enums.ServiceOrderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class DashboardRepositoryTest {

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private ReceivableRepository receivableRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("getRealizedRevenueSummary — Deve somar pagamentos, descontar estornos e separar período atual e anterior")
    void shouldSummarizeRealizedRevenueForCurrentAndPreviousPeriod() {
        Long companyId = 1L;
        LocalDateTime prevStart = LocalDateTime.of(2026, 10, 9, 0, 0);
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 23, 23, 59, 59, 999999999);

        saveTransactionWithPaidAt(companyId, PaymentTransactionType.PAYMENT, new BigDecimal("1000.00"), LocalDateTime.of(2026, 11, 5, 10, 0));
        saveTransactionWithPaidAt(companyId, PaymentTransactionType.PAYMENT, new BigDecimal("500.50"), LocalDateTime.of(2026, 11, 10, 14, 0));
        saveTransactionWithPaidAt(companyId, PaymentTransactionType.REFUND, new BigDecimal("200.50"), LocalDateTime.of(2026, 11, 12, 16, 0));

        saveTransactionWithPaidAt(companyId, PaymentTransactionType.PAYMENT, new BigDecimal("900.00"), LocalDateTime.of(2026, 10, 15, 11, 0));
        saveTransactionWithPaidAt(companyId, PaymentTransactionType.REFUND, new BigDecimal("100.00"), LocalDateTime.of(2026, 10, 20, 12, 0));

        // Fora do período e outra empresa (devem ser ignorados)
        saveTransactionWithPaidAt(companyId, PaymentTransactionType.PAYMENT, new BigDecimal("999.00"), LocalDateTime.of(2026, 10, 1, 9, 0));
        saveTransactionWithPaidAt(2L, PaymentTransactionType.PAYMENT, new BigDecimal("888.00"), LocalDateTime.of(2026, 11, 10, 9, 0));

        entityManager.flush();
        entityManager.clear();

        RealizedRevenueSummary summary = paymentTransactionRepository.getRealizedRevenueSummary(
                companyId, start, end, prevStart
        );

        assertNotNull(summary);
        assertEquals(0, new BigDecimal("1300.00").compareTo(summary.currentRevenue()));
        assertEquals(2L, summary.paymentsCount());
        assertEquals(0, new BigDecimal("800.00").compareTo(summary.previousRevenue()));
    }

    @Test
    @DisplayName("generatedSales queries — Deve somar pedidos avulsos e OS sem duplicar OS convertida em pedido")
    void shouldSummarizeStandaloneOrdersAndServiceOrdersWithoutDuplication() {
        Long companyId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 23, 23, 59, 59, 999999999);

        // Pedido avulso válido
        OrderEntity standaloneOrder = saveOrderWithCreatedAt(companyId, OrderStatus.COMPLETED, new BigDecimal("1000.00"), LocalDateTime.of(2026, 11, 5, 10, 0));
        // Pedido cancelado (ignorado)
        saveOrderWithCreatedAt(companyId, OrderStatus.CANCELED, new BigDecimal("500.00"), LocalDateTime.of(2026, 11, 6, 10, 0));
        // Pedido convertido a partir de OS (não deve entrar na query de pedidos)
        OrderEntity convertedOrder = saveOrderWithCreatedAt(companyId, OrderStatus.OPEN, new BigDecimal("750.00"), LocalDateTime.of(2026, 11, 7, 10, 0));

        // OS vinculada ao pedido convertido (entra apenas na query de OS)
        saveServiceOrderWithCreatedAt(companyId, convertedOrder.getId(), ServiceOrderStatus.COMPLETED, new BigDecimal("750.00"), LocalDateTime.of(2026, 11, 7, 9, 0));
        // OS em aberto (entra na query de OS)
        saveServiceOrderWithCreatedAt(companyId, null, ServiceOrderStatus.IN_PROGRESS, new BigDecimal("350.00"), LocalDateTime.of(2026, 11, 8, 9, 0));
        // OS cancelada (ignorada)
        saveServiceOrderWithCreatedAt(companyId, null, ServiceOrderStatus.CANCELED, new BigDecimal("200.00"), LocalDateTime.of(2026, 11, 9, 9, 0));

        entityManager.flush();
        entityManager.clear();

        SalesAggregationSummary ordersSummary = orderRepository.getStandaloneOrdersSalesSummary(
                companyId, OrderStatus.CANCELED, start, end
        );
        SalesAggregationSummary serviceOrdersSummary = serviceOrderRepository.getServiceOrdersSalesSummary(
                companyId, ServiceOrderStatus.CANCELED, start, end
        );

        assertNotNull(standaloneOrder.getId());
        assertEquals(0, new BigDecimal("1000.00").compareTo(ordersSummary.totalAmount()));
        assertEquals(1L, ordersSummary.count());

        assertEquals(0, new BigDecimal("1100.00").compareTo(serviceOrdersSummary.totalAmount()));
        assertEquals(2L, serviceOrdersSummary.count());
    }

    @Test
    @DisplayName("status-summary queries — Deve agrupar status e somar valores totais de pedidos e OSs por período")
    void shouldSummarizeOrderAndServiceOrderStatusesByDateRange() {
        Long companyId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 23, 23, 59, 59, 999999999);

        saveOrderWithCreatedAt(companyId, OrderStatus.CANCELED, new BigDecimal("100.00"), LocalDateTime.of(2026, 11, 5, 10, 0));
        saveOrderWithCreatedAt(companyId, OrderStatus.COMPLETED, new BigDecimal("200.00"), LocalDateTime.of(2026, 11, 6, 10, 0));
        saveOrderWithCreatedAt(companyId, OrderStatus.COMPLETED, new BigDecimal("300.50"), LocalDateTime.of(2026, 11, 7, 10, 0));
        saveOrderWithCreatedAt(companyId, OrderStatus.CANCELED, new BigDecimal("50.00"), LocalDateTime.of(2026, 10, 20, 10, 0));
        saveOrderWithCreatedAt(2L, OrderStatus.CANCELED, new BigDecimal("50.00"), LocalDateTime.of(2026, 11, 8, 10, 0));

        saveServiceOrderWithCreatedAt(companyId, null, ServiceOrderStatus.CANCELED, new BigDecimal("100.00"), LocalDateTime.of(2026, 11, 5, 10, 0));
        saveServiceOrderWithCreatedAt(companyId, null, ServiceOrderStatus.IN_PROGRESS, new BigDecimal("150.25"), LocalDateTime.of(2026, 11, 6, 10, 0));
        saveServiceOrderWithCreatedAt(companyId, null, ServiceOrderStatus.IN_PROGRESS, new BigDecimal("250.75"), LocalDateTime.of(2026, 11, 7, 10, 0));
        saveServiceOrderWithCreatedAt(companyId, null, ServiceOrderStatus.CANCELED, new BigDecimal("80.00"), LocalDateTime.of(2026, 10, 15, 10, 0));
        saveServiceOrderWithCreatedAt(2L, null, ServiceOrderStatus.CANCELED, new BigDecimal("80.00"), LocalDateTime.of(2026, 11, 8, 10, 0));

        entityManager.flush();
        entityManager.clear();

        Map<OrderStatus, OrderStatusTotalSummary> ordersByStatus = orderRepository.summarizeWithTotalsByStatusesAndDateRange(companyId, start, end)
                .stream()
                .collect(Collectors.toMap(OrderStatusTotalSummary::status, Function.identity()));

        Map<ServiceOrderStatus, ServiceOrderStatusSummary> serviceOrdersByStatus = serviceOrderRepository.summarizeByStatusesAndDateRange(companyId, start, end)
                .stream()
                .collect(Collectors.toMap(ServiceOrderStatusSummary::status, Function.identity()));

        assertEquals(2, ordersByStatus.size());
        assertEquals(1L, ordersByStatus.get(OrderStatus.CANCELED).count());
        assertEquals(0, new BigDecimal("100.00").compareTo(ordersByStatus.get(OrderStatus.CANCELED).totalAmount()));
        assertEquals(2L, ordersByStatus.get(OrderStatus.COMPLETED).count());
        assertEquals(0, new BigDecimal("500.50").compareTo(ordersByStatus.get(OrderStatus.COMPLETED).totalAmount()));

        assertEquals(2, serviceOrdersByStatus.size());
        assertEquals(1L, serviceOrdersByStatus.get(ServiceOrderStatus.CANCELED).count());
        assertEquals(0, new BigDecimal("100.00").compareTo(serviceOrdersByStatus.get(ServiceOrderStatus.CANCELED).totalAmount()));
        assertEquals(2L, serviceOrdersByStatus.get(ServiceOrderStatus.IN_PROGRESS).count());
        assertEquals(0, new BigDecimal("401.00").compareTo(serviceOrdersByStatus.get(ServiceOrderStatus.IN_PROGRESS).totalAmount()));
    }

    @Test
    @DisplayName("getReceivablesKpiSummary — Deve calcular valores em aberto, vencendo em 7 dias e vencidos com contagem distinta de clientes")
    void shouldSummarizeOpenAndOverdueReceivables() {
        Long companyId = 1L;
        LocalDate today = LocalDate.of(2026, 11, 23);
        LocalDate weekEnd = today.plusDays(7);
        List<ReceivableStatus> openStatuses = List.of(
                ReceivableStatus.PENDING,
                ReceivableStatus.PARTIALLY_PAID,
                ReceivableStatus.OVERDUE
        );

        // Em aberto e vencendo nesta semana (today + 3 dias)
        saveReceivable(companyId, 101L, today.plusDays(3), new BigDecimal("500.00"), ReceivableStatus.PENDING);
        // Em aberto e vencendo hoje (today)
        saveReceivable(companyId, 102L, today, new BigDecimal("250.00"), ReceivableStatus.PARTIALLY_PAID);
        // Em aberto após 7 dias (today + 15 dias)
        saveReceivable(companyId, 103L, today.plusDays(15), new BigDecimal("1000.00"), ReceivableStatus.PENDING);

        // Vencidos (2 parcelas do cliente 201, 1 parcela do cliente 202 -> 3 parcelas, 2 clientes)
        saveReceivable(companyId, 201L, today.minusDays(5), new BigDecimal("300.00"), ReceivableStatus.OVERDUE);
        saveReceivable(companyId, 201L, today.minusDays(2), new BigDecimal("200.00"), ReceivableStatus.PENDING);
        saveReceivable(companyId, 202L, today.minusDays(10), new BigDecimal("400.00"), ReceivableStatus.PARTIALLY_PAID);

        // Ignorados: PAID, CANCELED, pendingAmount = 0, outra empresa
        saveReceivable(companyId, 203L, today.minusDays(1), new BigDecimal("999.00"), ReceivableStatus.PAID);
        saveReceivable(companyId, 204L, today.plusDays(1), new BigDecimal("999.00"), ReceivableStatus.CANCELED);
        saveReceivable(companyId, 205L, today.plusDays(1), BigDecimal.ZERO, ReceivableStatus.PENDING);
        saveReceivable(2L, 206L, today.minusDays(1), new BigDecimal("999.00"), ReceivableStatus.OVERDUE);

        entityManager.flush();
        entityManager.clear();

        ReceivablesKpiSummary summary = receivableRepository.getReceivablesKpiSummary(
                companyId, today, weekEnd, openStatuses
        );

        assertNotNull(summary);
        assertEquals(0, new BigDecimal("1750.00").compareTo(summary.openAmount()));
        assertEquals(3L, summary.openInstallmentsCount());
        assertEquals(0, new BigDecimal("750.00").compareTo(summary.dueThisWeekAmount()));
        assertEquals(2L, summary.dueThisWeekCount());
        assertEquals(0, new BigDecimal("900.00").compareTo(summary.overdueAmount()));
        assertEquals(3L, summary.overdueInstallmentsCount());
        assertEquals(2L, summary.overdueCustomersCount());
    }

    private void saveTransactionWithPaidAt(Long companyId, PaymentTransactionType type, BigDecimal amount, LocalDateTime paidAt) {
        PaymentTransactionEntity entity = paymentTransactionRepository.saveAndFlush(
                PaymentTransactionEntity.builder()
                        .receivableId(1L)
                        .companyId(companyId)
                        .transactionType(type)
                        .amountPaid(amount)
                        .paymentMethod(PaymentMethod.PIX)
                        .build()
        );
        entityManager.createNativeQuery("UPDATE payment_transactions SET paid_at = :paidAt WHERE id = :id")
                .setParameter("paidAt", paidAt)
                .setParameter("id", entity.getId())
                .executeUpdate();
    }

    private OrderEntity saveOrderWithCreatedAt(Long companyId, OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {
        OrderEntity entity = orderRepository.saveAndFlush(
                OrderEntity.builder()
                        .companyId(companyId)
                        .partnerId(1L)
                        .status(status)
                        .totalAmount(totalAmount)
                        .build()
        );
        entityManager.createNativeQuery("UPDATE orders SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", entity.getId())
                .executeUpdate();
        return entity;
    }

    private void saveServiceOrderWithCreatedAt(Long companyId, Long orderId, ServiceOrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {
        ServiceOrderEntity entity = serviceOrderRepository.saveAndFlush(
                ServiceOrderEntity.builder()
                        .companyId(companyId)
                        .partnerId(1L)
                        .orderId(orderId)
                        .status(status)
                        .objectTitle("Notebook")
                        .reportedIssue("Não liga")
                        .totalAmount(totalAmount)
                        .entryAt(createdAt)
                        .build()
        );
        entityManager.createNativeQuery("UPDATE service_orders SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", entity.getId())
                .executeUpdate();
    }

    private void saveReceivable(Long companyId, Long partnerId, LocalDate dueDate, BigDecimal pendingAmount, ReceivableStatus status) {
        receivableRepository.saveAndFlush(
                ReceivableEntity.builder()
                        .companyId(companyId)
                        .partnerId(partnerId)
                        .originType(ReceivableOriginType.MANUAL)
                        .dueDate(dueDate)
                        .amount(pendingAmount.max(new BigDecimal("100.00")))
                        .pendingAmount(pendingAmount)
                        .paymentMethod(PaymentMethod.PIX)
                        .status(status)
                        .build()
        );
    }
}
