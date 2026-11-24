package company.aura.erp.service;

import company.aura.erp.dto.job.ReceivableDueNotice;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.modules.email.domain.template.*;
import company.aura.erp.modules.email.enumerate.AttachmentSource;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.modules.email.service.EmailService;
import company.aura.erp.repository.ReceivableRepository;
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

    private final ReceivableRepository receivableRepository;
    private final EmailService emailService;
    private final ExecutorService executorService;

    private static final int DAYS_AHEAD = 2;
    private static final int PAGE_SIZE = 50;

    @Async
    @Transactional(readOnly = true)
    public void notifyUpcomingReceivables() {
        LocalDate targetDueDate = LocalDate.now().plusDays(DAYS_AHEAD);
        log.info("notifyUpcomingReceivables() - START - targetDueDate={}", targetDueDate);

        int pageNumber = 0;
        Page<ReceivableDueNotice> page;

        long totalProcessed = 0;
        long emailsSent = 0;

        do {
            log.info("notifyUpcomingReceivables() - Processing page - pageNumber={}, pageSize={}", pageNumber, PAGE_SIZE);
            page = getReceivablesToProcess(pageNumber, targetDueDate);
            totalProcessed += page.getNumberOfElements();

            List<CompletableFuture<Boolean>> futures = page.getContent().stream()
                    .map(notice -> CompletableFuture.supplyAsync(() -> sendBillingNotification(notice), executorService))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            emailsSent += futures.stream().map(CompletableFuture::join).filter(Boolean.TRUE::equals).count();

            pageNumber++;
        } while (page.hasNext());

        log.info("notifyUpcomingReceivables() - END - targetDueDate={}, totalProcessed={}, emailsSent={}", targetDueDate, totalProcessed, emailsSent);
    }

    private Page<ReceivableDueNotice> getReceivablesToProcess(int pageNumber, LocalDate targetDueDate) {
        log.info("getReceivablesToProcess() - START - pageNumber={}, targetDueDate={}", pageNumber, targetDueDate);
        List<ReceivableStatus> excludedStatuses = List.of(ReceivableStatus.PAID, ReceivableStatus.CANCELED);
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
        Page<ReceivableDueNotice> page = receivableRepository.findReceivablesDueNotice(targetDueDate, excludedStatuses, pageable);
        log.info("getReceivablesToProcess() - END - pageNumber={}, totalElements={}, totalPages={}", pageNumber, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    private boolean sendBillingNotification(ReceivableDueNotice receivable) {
        log.info("sendBillingNotification() - START - receivableId={}, orderId={}, serviceOrderId={}, companyId={}",
                receivable.receivableId(), receivable.orderId(), receivable.serviceOrderId(), receivable.companyId());
        try {
            NotificationRequest request = new NotificationRequest();
            request.setCompany(new Company());
            request.getCompany().setId(receivable.companyId());

            request.setCustomer(new Customer());
            request.getCustomer().setName(receivable.customerName());

            request.setReceiveEmail(StringUtils.defaultIfBlank(receivable.customerBillingEmail(), receivable.customerEmail()));

            request.setPayload(new Payload());
            request.getPayload().setAmount(receivable.amount());
            request.getPayload().setInstallmentDueDate(receivable.dueDate());
            request.getPayload().setInstallmentNumber(receivable.installmentNumber());
            request.getPayload().setOrderId(receivable.orderId());
            request.getPayload().setPaymentMethod(receivable.paymentMethod().getHumanDescription());

            if (StringUtils.isNotBlank(receivable.paymentFileUrl())) {
                Attachment attachment = new Attachment();
                attachment.setSource(AttachmentSource.BUCKET);
                attachment.setUrl(receivable.paymentFileUrl());
                request.setAttachment(List.of(attachment));
            }

            emailService.sendNotification(NotificationType.INSTALLMENT_NOTIFY, request);
            log.info("sendBillingNotification() - END - receivableId={}, orderId={}", receivable.receivableId(), receivable.orderId());
            return true;
        } catch (Exception ex) {
            log.error("sendBillingNotification() - ERROR - Error when sending billing notification - receivableId={}, orderId={}, error: {}",
                    receivable.receivableId(), receivable.orderId(), ex.getMessage(), ex);
            return false;
        }
    }
}
