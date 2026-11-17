package company.aura.erp.service;

import company.aura.erp.dto.job.OrderInstallmentDueNotice;
import company.aura.erp.email.domain.Company;
import company.aura.erp.email.domain.Customer;
import company.aura.erp.email.domain.NotificationRequest;
import company.aura.erp.email.domain.Payload;
import company.aura.erp.email.enumerate.NotificationType;
import company.aura.erp.email.service.EmailService;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.repository.OrderInstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final OrderInstallmentRepository orderInstallmentRepository;
    private final EmailService emailService;
    private final ExecutorService executorService;

    private static final int DAYS_AHEAD = 2;
    private static final int PAGE_SIZE = 50;


    @Async
    @Transactional(readOnly = true)
    public void notifyUpcomingInstallments() {
        LocalDate targetDueDate = LocalDate.now().plusDays(DAYS_AHEAD);
        log.info("notifyUpcomingInstallments() - START - targetDueDate={}", targetDueDate);

        int pageNumber = 0;
        Page<OrderInstallmentDueNotice> page;

        long emailsSent = 0;

        do {
            page = getInstallmentsToProcess(pageNumber, targetDueDate);

            List<CompletableFuture<Boolean>> futures = page.getContent().stream()
                    .map(notice-> CompletableFuture.supplyAsync(() -> sendBillingNotification(notice), executorService))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            emailsSent += futures.stream().map(CompletableFuture::join).filter(Boolean.TRUE::equals).count();

            pageNumber++;
        } while (page.hasNext());

        log.info("notifyUpcomingInstallments() - END - targetDueDate={}, totalProcessed={}, emailsSent={}", targetDueDate, page.getTotalElements(), emailsSent);
    }

    private Page<OrderInstallmentDueNotice> getInstallmentsToProcess(int pageNumber, LocalDate targetDueDate) {
        List<InstallmentStatus> excludedStatuses = List.of(InstallmentStatus.PAID, InstallmentStatus.CANCELED);
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        return orderInstallmentRepository.findInstallmentsDueNotice(targetDueDate, excludedStatuses, pageable);
    }


    private boolean sendBillingNotification(OrderInstallmentDueNotice installment) {
        try {
            NotificationRequest request = new NotificationRequest();
            request.setCompany(new Company());
            request.getCompany().setId(installment.companyId());

            request.setCustomer(new Customer());
            request.getCustomer().setName(installment.customerName());

            request.setReceiveEmail(StringUtils.defaultIfBlank(installment.customerBillingEmail(), installment.customerEmail()));

            request.setPayload(new Payload());
            request.getPayload().setAmount(installment.amount());
            request.getPayload().setInstallmentDueDate(installment.dueDate());
            request.getPayload().setInstallmentNumber(installment.installmentNumber());
            request.getPayload().setOrderId(installment.orderId());
            request.getPayload().setPaymentMethod(installment.paymentMethod().getHumanDescription());

            emailService.sendNotification(NotificationType.INSTALLMENT_NOTIFY, request);
            return true;
        } catch (Exception ex) {
            log.error("sendBillingNotification() - Unexpected error - installment: {} - error: {}", installment, ex.getMessage(), ex);
            return false;
        }
    }
}
