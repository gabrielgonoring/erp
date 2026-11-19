package company.aura.erp.service;

import company.aura.erp.dto.job.OrderInstallmentDueNotice;
import company.aura.erp.modules.email.domain.template.*;
import company.aura.erp.modules.email.enumerate.AttachmentSource;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.modules.email.service.EmailService;
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

        long totalProcessed = 0;
        long emailsSent = 0;

        do {
            log.info("notifyUpcomingInstallments() - Processing page - pageNumber={}, pageSize={}", pageNumber, PAGE_SIZE);
            page = getInstallmentsToProcess(pageNumber, targetDueDate);
            totalProcessed += page.getNumberOfElements();

            List<CompletableFuture<Boolean>> futures = page.getContent().stream()
                    .map(notice -> CompletableFuture.supplyAsync(() -> sendBillingNotification(notice), executorService))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            emailsSent += futures.stream().map(CompletableFuture::join).filter(Boolean.TRUE::equals).count();

            pageNumber++;
        } while (page.hasNext());

        log.info("notifyUpcomingInstallments() - END - targetDueDate={}, totalProcessed={}, emailsSent={}", targetDueDate, totalProcessed, emailsSent);
    }

    private Page<OrderInstallmentDueNotice> getInstallmentsToProcess(int pageNumber, LocalDate targetDueDate) {
        log.info("getInstallmentsToProcess() - START - pageNumber={}, targetDueDate={}", pageNumber, targetDueDate);
        List<InstallmentStatus> excludedStatuses = List.of(InstallmentStatus.PAID, InstallmentStatus.CANCELED);
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        Page<OrderInstallmentDueNotice> page = orderInstallmentRepository.findInstallmentsDueNotice(targetDueDate, excludedStatuses, pageable);
        log.info("getInstallmentsToProcess() - END - pageNumber={}, totalElements={}, totalPages={}", pageNumber, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    private boolean sendBillingNotification(OrderInstallmentDueNotice installment) {
        log.info("sendBillingNotification() - START - installmentId={}, orderId={}, companyId={}", installment.installmentId(), installment.orderId(), installment.companyId());
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

            if (StringUtils.isNotBlank(installment.bankSlipUrl())) {
                Attachment attachment = new Attachment();
                attachment.setSource(AttachmentSource.BUCKET);
                attachment.setUrl(installment.bankSlipUrl());
                request.setAttachment(List.of(attachment));
            }

            emailService.sendNotification(NotificationType.INSTALLMENT_NOTIFY, request);
            log.info("sendBillingNotification() - END - installmentId={}, orderId={}", installment.installmentId(), installment.orderId());
            return true;
        } catch (Exception ex) {
            log.error("sendBillingNotification() - ERROR - Error when sending billing notification - installmentId={}, orderId={}, error: {}", installment.installmentId(), installment.orderId(), ex.getMessage(), ex);
            return false;
        }
    }
}
