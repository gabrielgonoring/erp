package company.aura.erp.service;

import company.aura.erp.dto.dashboard.*;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.enums.ServiceOrderStatus;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
import company.aura.erp.repository.ReceivableRepository;
import company.aura.erp.repository.ServiceOrderRepository;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.alert.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<ReceivableStatus> OPEN_RECEIVABLE_STATUSES = List.of(
            ReceivableStatus.PENDING,
            ReceivableStatus.PARTIALLY_PAID,
            ReceivableStatus.OVERDUE
    );

    private final OrderRepository orderRepository;

    private final ServiceOrderRepository serviceOrderRepository;

    private final ReceivableRepository receivableRepository;

    private final PaymentTransactionRepository paymentTransactionRepository;

    private final List<AlertService> alertsValidations;

    private final ExecutorService executorService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate startDate, LocalDate endDate, Long companyId) {
        log.info("getSummary() - START - companyId={}, startDate={}, endDate={}", companyId, startDate, endDate);

        LocalDate today = LocalDate.now();
        LocalDateTime resolvedStartDate = startDate != null ? startDate.atStartOfDay() : today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime resolvedEndDate = endDate != null ? endDate.atTime(LocalTime.MAX) : today.atTime(LocalTime.MAX);

        validateSummaryRequest(resolvedStartDate, resolvedEndDate);

        CompletableFuture<List<OrderStatusSummary>> statusSummary = CompletableFuture.supplyAsync(
                () -> getSalesResponseByOrderStatus(companyId, resolvedStartDate, resolvedEndDate),
                executorService
        );

        CompletableFuture<ReceivablePaymentSummary> paymentSummary = CompletableFuture.supplyAsync(
                () -> getPaymentSummary(companyId, resolvedStartDate, resolvedEndDate),
                executorService
        );

        CompletableFuture<PendingReceivableSummary> pendingReceivablesFuture = CompletableFuture.supplyAsync(
                () -> getPendingReceivablesSummary(companyId, resolvedStartDate.toLocalDate(), resolvedEndDate.toLocalDate()),
                executorService
        );

        CompletableFuture<PaymentTransactionSummary> revenue = CompletableFuture.supplyAsync(
                () -> getPaymentForPeriod(companyId, resolvedStartDate, resolvedEndDate),
                executorService
        );

        try {
            CompletableFuture.allOf(paymentSummary, statusSummary, pendingReceivablesFuture, revenue).join();

            var salesPerStatus = statusSummary.join();
            var paymentSummaryResponse = paymentSummary.join();
            var pendingReceivablesResponse = pendingReceivablesFuture.join();
            var revenueResponse = revenue.join();

            DashboardSummaryResponse response = new DashboardSummaryResponse(
                    resolvedStartDate.toLocalDate(),
                    resolvedEndDate.toLocalDate(),
                    salesPerStatus,
                    paymentSummaryResponse.paidAmount(),
                    paymentSummaryResponse.pendingAmount(),
                    paymentSummaryResponse.paidAmount().add(paymentSummaryResponse.pendingAmount()),
                    pendingReceivablesResponse,
                    revenueResponse
            );

            log.info("getSummary() - END - companyId={}, startDate={}, endDate={}", companyId, response.startDate(), response.endDate());
            return response;

        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public DashboardKpisResponse getKpis(LocalDate startDate, LocalDate endDate, Long companyId) {
        log.info("getKpis() - START - companyId={}, startDate={}, endDate={}", companyId, startDate, endDate);

        LocalDate today = LocalDate.now();
        LocalDate resolvedStartDate = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate resolvedEndDate = endDate != null ? endDate : today;

        LocalDateTime resolvedStartDateTime = resolvedStartDate.atStartOfDay();
        LocalDateTime resolvedEndDateTime = resolvedEndDate.atTime(LocalTime.MAX);

        validateSummaryRequest(resolvedStartDateTime, resolvedEndDateTime);

        long periodDays = ChronoUnit.DAYS.between(resolvedStartDate, resolvedEndDate);
        LocalDate prevEndDate = resolvedStartDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(periodDays);
        LocalDateTime prevStartDateTime = prevStartDate.atStartOfDay();
        LocalDate weekEndDate = today.plusDays(7);

        CompletableFuture<RealizedRevenueSummary> realizedRevenueFuture = CompletableFuture.supplyAsync(
                () -> getRealizedRevenueSummary(companyId, resolvedStartDateTime, resolvedEndDateTime, prevStartDateTime),
                executorService
        );

        CompletableFuture<SalesAggregationSummary> ordersSalesFuture = CompletableFuture.supplyAsync(
                () -> getStandaloneOrdersSalesSummary(companyId, resolvedStartDateTime, resolvedEndDateTime),
                executorService
        );

        CompletableFuture<SalesAggregationSummary> serviceOrdersSalesFuture = CompletableFuture.supplyAsync(
                () -> getServiceOrdersSalesSummary(companyId, resolvedStartDateTime, resolvedEndDateTime),
                executorService
        );

        CompletableFuture<ReceivablesKpiSummary> receivablesKpiFuture = CompletableFuture.supplyAsync(
                () -> getReceivablesKpiSummary(companyId, today, weekEndDate),
                executorService
        );

        try {
            CompletableFuture.allOf(
                    realizedRevenueFuture,
                    ordersSalesFuture,
                    serviceOrdersSalesFuture,
                    receivablesKpiFuture
            ).join();

            DashboardKpisResponse response = buildKpiResponse(realizedRevenueFuture, ordersSalesFuture, serviceOrdersSalesFuture, receivablesKpiFuture, resolvedStartDate, resolvedEndDate);

            log.info("getKpis() - END - companyId={}, startDate={}, endDate={}", companyId, resolvedStartDate, resolvedEndDate);
            return response;

        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private DashboardKpisResponse buildKpiResponse(CompletableFuture<RealizedRevenueSummary> realizedRevenueFuture, CompletableFuture<SalesAggregationSummary> ordersSalesFuture, CompletableFuture<SalesAggregationSummary> serviceOrdersSalesFuture, CompletableFuture<ReceivablesKpiSummary> receivablesKpiFuture, LocalDate resolvedStartDate, LocalDate resolvedEndDate) {
        RealizedRevenueSummary realizedRevenueSummary = realizedRevenueFuture.join();
        SalesAggregationSummary ordersSalesSummary = ordersSalesFuture.join();
        SalesAggregationSummary serviceOrdersSalesSummary = serviceOrdersSalesFuture.join();
        ReceivablesKpiSummary receivablesKpiSummary = receivablesKpiFuture.join();

        BigDecimal currentRevenue = scaleAmount(realizedRevenueSummary.currentRevenue());
        BigDecimal previousRevenue = scaleAmount(realizedRevenueSummary.previousRevenue());
        BigDecimal growthPercentage = calculateGrowthPercentage(currentRevenue, previousRevenue);

        BigDecimal ordersAmount = scaleAmount(ordersSalesSummary.totalAmount());
        BigDecimal serviceOrdersAmount = scaleAmount(serviceOrdersSalesSummary.totalAmount());
        BigDecimal totalSalesAmount = ordersAmount.add(serviceOrdersAmount);

        DashboardKpisResponse response = new DashboardKpisResponse(
                new DashboardKpisResponse.Period(resolvedStartDate, resolvedEndDate),
                new DashboardKpisResponse.Kpis(
                        new DashboardKpisResponse.RealizedRevenue(
                                currentRevenue,
                                previousRevenue,
                                growthPercentage,
                                realizedRevenueSummary.paymentsCount()
                        ),
                        new DashboardKpisResponse.GeneratedSales(
                                totalSalesAmount,
                                ordersAmount,
                                ordersSalesSummary.count(),
                                serviceOrdersAmount,
                                serviceOrdersSalesSummary.count()
                        ),
                        new DashboardKpisResponse.OpenReceivables(
                                scaleAmount(receivablesKpiSummary.openAmount()),
                                receivablesKpiSummary.openInstallmentsCount(),
                                scaleAmount(receivablesKpiSummary.dueThisWeekAmount()),
                                receivablesKpiSummary.dueThisWeekCount()
                        ),
                        new DashboardKpisResponse.OverdueReceivables(
                                scaleAmount(receivablesKpiSummary.overdueAmount()),
                                receivablesKpiSummary.overdueInstallmentsCount(),
                                receivablesKpiSummary.overdueCustomersCount()
                        )
                )
        );
        return response;
    }

    private RealizedRevenueSummary getRealizedRevenueSummary(Long companyId, LocalDateTime startDate, LocalDateTime endDate, LocalDateTime prevStartDate) {
        RealizedRevenueSummary summary = paymentTransactionRepository.getRealizedRevenueSummary(
                companyId,
                startDate,
                endDate,
                prevStartDate
        );
        return summary != null ? summary : new RealizedRevenueSummary(BigDecimal.ZERO, 0L, BigDecimal.ZERO);
    }

    private SalesAggregationSummary getStandaloneOrdersSalesSummary(Long companyId, LocalDateTime startDate, LocalDateTime endDate) {
        SalesAggregationSummary summary = orderRepository.getStandaloneOrdersSalesSummary(
                companyId,
                OrderStatus.CANCELED,
                startDate,
                endDate
        );
        return summary != null ? summary : new SalesAggregationSummary(BigDecimal.ZERO, 0L);
    }

    private SalesAggregationSummary getServiceOrdersSalesSummary(Long companyId, LocalDateTime startDate, LocalDateTime endDate) {
        SalesAggregationSummary summary = serviceOrderRepository.getServiceOrdersSalesSummary(
                companyId,
                ServiceOrderStatus.CANCELED,
                startDate,
                endDate
        );
        return summary != null ? summary : new SalesAggregationSummary(BigDecimal.ZERO, 0L);
    }

    private ReceivablesKpiSummary getReceivablesKpiSummary(Long companyId, LocalDate today, LocalDate weekEndDate) {
        ReceivablesKpiSummary summary = receivableRepository.getReceivablesKpiSummary(
                companyId,
                today,
                weekEndDate,
                OPEN_RECEIVABLE_STATUSES
        );
        return summary != null ? summary : new ReceivablesKpiSummary(
                BigDecimal.ZERO, 0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO, 0L, 0L
        );
    }

    private BigDecimal scaleAmount(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGrowthPercentage(BigDecimal currentAmount, BigDecimal previousAmount) {
        if (previousAmount == null || previousAmount.compareTo(BigDecimal.ZERO) == 0) {
            if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
            }
            return currentAmount.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(100.0)
                    : BigDecimal.valueOf(-100.0);
        }
        return currentAmount.subtract(previousAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousAmount.abs(), 1, RoundingMode.HALF_UP);
    }

    private List<OrderStatusSummary> getSalesResponseByOrderStatus(Long companyId, LocalDateTime resolvedStartDate, LocalDateTime resolvedEndDate) {
        return orderRepository.summarizeByStatusesAndDateRange(companyId, resolvedStartDate, resolvedEndDate);
    }

    private PaymentTransactionSummary getPaymentForPeriod(Long companyId, LocalDateTime resolvedStartDate, LocalDateTime resolvedEndDate) {
        return paymentTransactionRepository.getPaymentsSummaryByStatusInAndDateRange(
                companyId,
                List.of(ReceivableStatus.PAID, ReceivableStatus.PARTIALLY_PAID),
                resolvedStartDate,
                resolvedEndDate
        );
    }

    private ReceivablePaymentSummary getPaymentSummary(Long companyId, LocalDateTime resolvedStartDate, LocalDateTime resolvedEndDate) {
        return receivableRepository.getPaymentsSummaryByOrderDate(
                companyId,
                List.of(OrderStatus.COMPLETED),
                List.of(ReceivableStatus.CANCELED),
                resolvedStartDate,
                resolvedEndDate
        );
    }

    private PendingReceivableSummary getPendingReceivablesSummary(Long companyId, LocalDate startDate, LocalDate endDate) {
        PendingReceivableSummary summary = receivableRepository.getPendingReceivablesSummaryByDueDate(companyId, startDate, endDate);
        return summary != null ? summary : new PendingReceivableSummary(0L, BigDecimal.ZERO);
    }

    private void validateSummaryRequest(LocalDateTime resolvedStartDate, LocalDateTime resolvedEndDate) {
        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }
    }

    public List<SystemAlertResponse> getAlerts(UserPrincipal userPrincipal) {
        log.info("getAlerts() - START - userId={}, companyId={}", userPrincipal != null ? userPrincipal.getId() : null, userPrincipal != null ? userPrincipal.getCompanyId() : null);

        if (alertsValidations == null || alertsValidations.isEmpty()) {
            log.info("getAlerts() - END - totalAlerts=0");
            return Collections.emptyList();
        }

        try {
            List<CompletableFuture<SystemAlertResponse>> futures = alertsValidations.stream()
                    .map(alertService -> CompletableFuture.supplyAsync(() -> alertService.getAlert(userPrincipal), executorService))
                    .toList();

            List<SystemAlertResponse> alerts = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

            log.info("getAlerts() - END - userId={}, companyId={}, totalAlerts={}", userPrincipal != null ? userPrincipal.getId() : null, userPrincipal != null ? userPrincipal.getCompanyId() : null, alerts.size());
            return alerts;
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    public List<SystemAlertResponse> getAlerts() {
        log.info("getAlerts() - START");
        List<SystemAlertResponse> alerts = getAlerts(null);
        log.info("getAlerts() - END - totalAlerts={}", alerts.size());
        return alerts;
    }
}
