package company.aura.erp.service;

import company.aura.erp.entity.CompanyEntity;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.event.InvoiceLinkedEvent;
import company.aura.erp.modules.email.domain.template.Attachment;
import company.aura.erp.modules.email.domain.template.Company;
import company.aura.erp.modules.email.domain.template.Customer;
import company.aura.erp.modules.email.domain.template.NotificationRequest;
import company.aura.erp.modules.email.domain.template.Payload;
import company.aura.erp.modules.email.enumerate.AttachmentSource;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.modules.email.service.EmailService;
import company.aura.erp.repository.CompanyRepository;
import company.aura.erp.repository.InvoiceRepository;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInvoiceNotificationService {

    private final OrderRepository orderRepository;
    private final PartnerRepository partnerRepository;
    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleInvoiceLinked(InvoiceLinkedEvent event) {
        if (event == null || event.companyId() == null || event.orderId() == null || event.invoiceId() == null) {
            log.warn("handleInvoiceLinked() - WARN - Invalid event cancelling notification - event={}", event);
            return;
        }

        log.info("handleInvoiceLinked() - START - companyId={}, orderId={}, invoiceId={}",
                event.companyId(), event.orderId(), event.invoiceId());

        try {
            Optional<OrderEntity> orderOpt = orderRepository.findByIdAndCompanyId(event.orderId(), event.companyId());
            if (orderOpt.isEmpty()) {
                log.warn("handleInvoiceLinked() - WARN - Order not found - orderId={}, companyId={}",
                        event.orderId(), event.companyId());
                return;
            }

            OrderEntity order = orderOpt.get();
            if (order.getPartnerId() == null) {
                log.warn("handleInvoiceLinked() - WARN - Order has no customer partnerId - orderId={}, companyId={}",
                        order.getId(), event.companyId());
                return;
            }

            Optional<PartnerEntity> partnerOpt = partnerRepository.findByIdAndCompanyId(order.getPartnerId(), event.companyId());
            if (partnerOpt.isEmpty()) {
                log.warn("handleInvoiceLinked() - WARN - Customer partner not found - partnerId={}, orderId={}, companyId={}",
                        order.getPartnerId(), order.getId(), event.companyId());
                return;
            }

            PartnerEntity partner = partnerOpt.get();
            String recipientEmail = StringUtils.defaultIfBlank(partner.getBillingEmail(), partner.getEmail());
            if (StringUtils.isBlank(recipientEmail)) {
                log.warn("handleInvoiceLinked() - WARN - Customer has no valid email cancelling notification - partnerId={}, orderId={}",
                        partner.getId(), order.getId());
                return;
            }

            Optional<InvoiceEntity> invoiceOpt = invoiceRepository.findByIdAndCompanyId(event.invoiceId(), event.companyId());
            if (invoiceOpt.isEmpty()) {
                log.warn("handleInvoiceLinked() - WARN - Invoice not found - invoiceId={}, orderId={}, companyId={}",
                        event.invoiceId(), order.getId(), event.companyId());
                return;
            }

            InvoiceEntity invoice = invoiceOpt.get();
            String companyName = resolveCompanyName(event.companyId());

            NotificationRequest request = buildNotificationRequest(
                    event.companyId(),
                    companyName,
                    recipientEmail.trim(),
                    partner,
                    order,
                    invoice
            );

            emailService.sendNotification(NotificationType.INVOICE_LINKED, request);

            log.info("handleInvoiceLinked() - END - orderId={}, invoiceId={}, recipientEmail={}",
                    order.getId(), invoice.getId(), request.getReceiveEmail());
        } catch (Exception ex) {
            log.error("handleInvoiceLinked() - ERROR - Error when sending invoice linked notification - orderId={}, invoiceId={}, error: {}",
                    event.orderId(), event.invoiceId(), ex.getMessage(), ex);
        }
    }

    private String resolveCompanyName(Long companyId) {
        return companyRepository.findById(companyId)
                .map(this::extractCompanyName)
                .orElse(null);
    }

    private String extractCompanyName(CompanyEntity company) {
        return StringUtils.defaultIfBlank(company.getTradeName(), company.getCorporateName());
    }

    private NotificationRequest buildNotificationRequest(
            Long companyId,
            String companyName,
            String recipientEmail,
            PartnerEntity partner,
            OrderEntity order,
            InvoiceEntity invoice
    ) {
        Company company = new Company();
        company.setId(companyId);
        company.setName(companyName);

        Customer customer = new Customer();
        customer.setName(partner.getName());

        Payload payload = new Payload();
        payload.setOrderId(order.getId());
        payload.setInvoiceId(invoice.getId());
        payload.setInvoiceNumber(resolveInvoiceNumber(invoice));
        payload.setDocumentType(invoice.getDocumentType() != null ? invoice.getDocumentType().name() : null);
        BigDecimal totalAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : order.getTotalAmount();
        payload.setAmount(totalAmount);

        List<Attachment> attachments = buildAttachments(invoice);

        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail(recipientEmail);
        request.setCompany(company);
        request.setCustomer(customer);
        request.setPayload(payload);
        request.setAttachment(attachments);
        return request;
    }

    private String resolveInvoiceNumber(InvoiceEntity invoice) {
        if (invoice.getInvoiceNumber() != null) {
            return String.valueOf(invoice.getInvoiceNumber());
        }
        return invoice.getId() != null ? String.valueOf(invoice.getId()) : null;
    }

    private List<Attachment> buildAttachments(InvoiceEntity invoice) {
        List<Attachment> attachments = new ArrayList<>();
        if (StringUtils.isNotBlank(invoice.getPdfUrl())) {
            attachments.add(Attachment.builder()
                    .source(AttachmentSource.BUCKET)
                    .url(invoice.getPdfUrl())
                    .build());
        }
        if (StringUtils.isNotBlank(invoice.getXmlUrl())) {
            attachments.add(Attachment.builder()
                    .source(AttachmentSource.BUCKET)
                    .url(invoice.getXmlUrl())
                    .build());
        }
        return attachments;
    }
}
