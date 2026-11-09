package company.aura.erp.service;

import company.aura.erp.dto.dashboard.DashboardSummaryResponse;
import company.aura.erp.dto.dashboard.DashboardSummaryResponse.PendingSalesResponse;
import company.aura.erp.dto.dashboard.DashboardSummaryResponse.SalesVolumeResponse;
import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.alert.AlertService;
import lombok.RequiredArgsConstructor;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;

    private final List<AlertService> alertsValidations;

    private final ExecutorService executorService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate startDate, LocalDate endDate, Long companyId) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedStartDate = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate resolvedEndDate = endDate != null ? endDate : today;

        validateSummaryRequest(resolvedStartDate, resolvedEndDate);

        CompletableFuture<SalesVolumeResponse> salesVolumeFuture = CompletableFuture.supplyAsync(
                () -> getSalesVolume(companyId, resolvedStartDate, resolvedEndDate),
                executorService
        );

        CompletableFuture<PendingSalesResponse> pendingSalesFuture = CompletableFuture.supplyAsync(
                () -> getPendingSales(companyId),
                executorService
        );

        CompletableFuture<BigDecimal> totalRevenueFuture = CompletableFuture.supplyAsync(
                () -> getTotalRevenue(companyId, today),
                executorService
        );

        try {
            CompletableFuture.allOf(salesVolumeFuture, pendingSalesFuture, totalRevenueFuture).join();

            SalesVolumeResponse salesVolume = salesVolumeFuture.join();
            PendingSalesResponse pendingSales = pendingSalesFuture.join();
            BigDecimal totalRevenue = totalRevenueFuture.join();

            return new DashboardSummaryResponse(
                    resolvedStartDate,
                    resolvedEndDate,
                    totalRevenue,
                    salesVolume,
                    pendingSales
            );
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private PendingSalesResponse getPendingSales(Long companyId) {
        // 3. Vendas em Aberto / Pendentes: Total em R$ e quantidade de pedidos aguardando faturamento ou pagamento
        List<OrderStatus> pendingStatuses = List.of(OrderStatus.OPEN, OrderStatus.PENDING_PAYMENT);

        long pendingOrdersCount = orderRepository.countByStatuses(companyId, pendingStatuses);

        BigDecimal pendingOrdersAmount = getPendingOrdersAmount(companyId, pendingStatuses);

        PendingSalesResponse pendingSales = new PendingSalesResponse(pendingOrdersAmount, pendingOrdersCount);

        return pendingSales;
    }

    private BigDecimal getPendingOrdersAmount(Long companyId, List<OrderStatus> pendingStatuses) {
        BigDecimal pendingOrdersAmount = orderRepository.sumAmountByStatuses(companyId, pendingStatuses);

        if (pendingOrdersAmount == null) {
            pendingOrdersAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            pendingOrdersAmount = pendingOrdersAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return pendingOrdersAmount;
    }

    private @NonNull SalesVolumeResponse getSalesVolume(Long companyId, LocalDate resolvedStartDate, LocalDate resolvedEndDate) {
        LocalDateTime periodStart = resolvedStartDate.atStartOfDay();
        LocalDateTime periodEnd = resolvedEndDate.atTime(LocalTime.MAX);


        BigDecimal closedOrdersAmount = getClosedOrdersAmount(companyId, periodStart, periodEnd);

        long closedOrdersCount = getClosedOrdersCount(companyId, periodStart, periodEnd);

        BigDecimal averageTicket = getAverageTicket(closedOrdersCount, closedOrdersAmount);

        SalesVolumeResponse salesVolume = new SalesVolumeResponse(closedOrdersCount, averageTicket);
        return salesVolume;
    }

    private BigDecimal getAverageTicket(long closedOrdersCount, BigDecimal closedOrdersAmount) {
        BigDecimal averageTicket = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (closedOrdersCount > 0) {
            averageTicket = closedOrdersAmount.divide(BigDecimal.valueOf(closedOrdersCount), 2, RoundingMode.HALF_UP);
        }
        return averageTicket;
    }

    private long getClosedOrdersCount(Long companyId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        // 2. Volume de Vendas: Quantidade de pedidos fechados no período e ticket médio por venda
        long closedOrdersCount = orderRepository.countByStatusAndDateRange(
                companyId,
                OrderStatus.COMPLETED,
                periodStart,
                periodEnd
        );
        return closedOrdersCount;
    }

    private @NonNull BigDecimal getClosedOrdersAmount(Long companyId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        BigDecimal closedOrdersAmount = orderRepository.sumAmountByStatusAndDateRange(
                companyId,
                OrderStatus.COMPLETED,
                periodStart,
                periodEnd
        );
        if (closedOrdersAmount == null) {
            closedOrdersAmount = BigDecimal.ZERO;
        }
        return closedOrdersAmount;
    }

    private @NonNull BigDecimal getTotalRevenue(Long companyId, LocalDate today) {
        // 1. Faturamento Total Realizado: Soma dos pedidos concluídos ou pagos no mês
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.atTime(LocalTime.MAX);
        BigDecimal totalRevenue = orderRepository.sumAmountByStatusAndDateRange(
                companyId,
                OrderStatus.COMPLETED,
                startOfMonth,
                endOfMonth
        );
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            totalRevenue = totalRevenue.setScale(2, RoundingMode.HALF_UP);
        }
        return totalRevenue;
    }

    private void validateSummaryRequest(LocalDate resolvedStartDate, LocalDate resolvedEndDate) {
        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }
    }

    public List<SystemAlertResponse> getAlerts(UserPrincipal userPrincipal) {
        if (alertsValidations == null || alertsValidations.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<CompletableFuture<SystemAlertResponse>> futures = alertsValidations.stream()
                    .map(alertService -> CompletableFuture.supplyAsync(() -> alertService.getAlert(userPrincipal), executorService))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    public List<SystemAlertResponse> getAlerts() {
        return getAlerts(null);
    }
}
