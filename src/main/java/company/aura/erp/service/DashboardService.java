package company.aura.erp.service;

import company.aura.erp.dto.dashboard.*;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
import company.aura.erp.repository.ReceivableRepository;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.alert.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private final OrderRepository orderRepository;

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
            CompletableFuture.allOf(paymentSummary, statusSummary, pendingReceivablesFuture).join();

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
