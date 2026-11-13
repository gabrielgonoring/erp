package company.aura.erp.service;

import company.aura.erp.dto.dashboard.*;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.repository.OrderInstallmentRepository;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.alert.AlertService;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;

    private final OrderInstallmentRepository orderInstallmentRepository;

    private final PaymentTransactionRepository paymentTransactionRepository;

    private final List<AlertService> alertsValidations;

    private final ExecutorService executorService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate startDate, LocalDate endDate, Long companyId) {
        LocalDate today = LocalDate.now();
        LocalDateTime resolvedStartDate = startDate != null ? startDate.atStartOfDay() : today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime resolvedEndDate = endDate != null ? endDate.atTime(LocalTime.MAX) : today.atTime(LocalTime.MAX);

        validateSummaryRequest(resolvedStartDate, resolvedEndDate);

        CompletableFuture<List<OrderStatusSummary>> statusSummary = CompletableFuture.supplyAsync(
                () -> getSalesResponseByOrderStatus(companyId, resolvedStartDate, resolvedEndDate),
                executorService
        );

        CompletableFuture<InstallmentPaymentSummary> paymentSummary = CompletableFuture.supplyAsync(
                () -> getPaymentSummary(companyId, resolvedStartDate, resolvedEndDate),
                executorService
        );

        CompletableFuture<PendingInstallmentSummary> pendingInstallmentsFuture = CompletableFuture.supplyAsync(
                () -> getPendingInstallmentsSummary(companyId, resolvedStartDate.toLocalDate(), resolvedEndDate.toLocalDate()),
                executorService
        );

        CompletableFuture<PaymentTransactionSummary> revenue = CompletableFuture.supplyAsync(
                () -> getPaymentForPeriod(companyId, resolvedStartDate, resolvedEndDate),
                executorService
        );

        try {
            CompletableFuture.allOf(paymentSummary, statusSummary, pendingInstallmentsFuture).join();

            var salesPerStatus = statusSummary.join();
            var paymentSummaryResponse = paymentSummary.join();
            var pendingInstallmentsResponse = pendingInstallmentsFuture.join();
            var revenueResponse = revenue.join();

            return new DashboardSummaryResponse(
                    resolvedStartDate.toLocalDate(),
                    resolvedEndDate.toLocalDate(),
                    salesPerStatus,
                    paymentSummaryResponse.paidAmount(),
                    paymentSummaryResponse.pendingAmount(),
                    paymentSummaryResponse.paidAmount().add(paymentSummaryResponse.pendingAmount()),
                    pendingInstallmentsResponse,
                    revenueResponse

            );

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
                List.of(InstallmentStatus.PAID, InstallmentStatus.PARTIALLY_PAID),
                resolvedStartDate,
                resolvedEndDate
        );
    }


    private InstallmentPaymentSummary getPaymentSummary(Long companyId, LocalDateTime resolvedStartDate, LocalDateTime resolvedEndDate) {
        return orderInstallmentRepository.getPaymentsSummaryByOrderDate(
                companyId,
                List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.COMPLETED),
                List.of(InstallmentStatus.CANCELED),
                resolvedStartDate,
                resolvedEndDate
        );
    }

    private PendingInstallmentSummary getPendingInstallmentsSummary(Long companyId, LocalDate startDate, LocalDate endDate) {
        PendingInstallmentSummary summary = orderInstallmentRepository.getPendingInstallmentsSummaryByDueDate(companyId, startDate, endDate);
        return summary != null ? summary : new PendingInstallmentSummary(0L, BigDecimal.ZERO);
    }


    private void validateSummaryRequest(LocalDateTime resolvedStartDate, LocalDateTime resolvedEndDate) {
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
