package company.aura.erp.service;

import company.aura.erp.entity.CompanyEntity;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.PartnerEntity;
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
    public void sendInvoiceToCustomer(Long companyId, Long orderId, Long invoiceId) {
        log.info("handleInvoiceLinked() - START - companyId={}, orderId={}, invoiceId={}", companyId, orderId, invoiceId);

        if (companyId == null || orderId == null || invoiceId == null) {
            log.warn("handleInvoiceLinked() - WARN - Invalid event cancelling notification");
            return;
        }

        try {
            Optional<OrderEntity> orderOpt = orderRepository.findByIdAndCompanyId(orderId, companyId);

            if (orderOpt.isEmpty()) {
                log.warn("handleInvoiceLinked() - WARN - Order not found - orderId={}, companyId={}", orderId, companyId);
                return;
            }

            OrderEntity order = orderOpt.get();

            if (order.getPartnerId() == null) {
                log.warn("handleInvoiceLinked() - WARN - Order has no customer partnerId - orderId={}, companyId={}", orderId, companyId);
                return;
            }

            Optional<PartnerEntity> partnerOpt = partnerRepository.findByIdAndCompanyId(order.getPartnerId(), companyId);

            if (partnerOpt.isEmpty()) {
                log.warn("handleInvoiceLinked() - WARN - Customer partner not found - partnerId={}, orderId={}, companyId={}", order.getPartnerId(), orderId, companyId);
                return;
            }

            PartnerEntity partner = partnerOpt.get();

            Optional<InvoiceEntity> invoiceOpt = invoiceRepository.findByIdAndCompanyId(invoiceId, companyId);

            if (invoiceOpt.isEmpty()) {
                log.warn("handleInvoiceLinked() - WARN - Invoice not found - invoiceId={}, orderId={}, companyId={}", invoiceId, orderId, companyId);
                return;
            }

            InvoiceEntity invoice = invoiceOpt.get();

            CompanyEntity company = resolveCompanyName(companyId);

            NotificationRequest request = buildNotificationRequest(company, partner, order, invoice);

            emailService.sendNotification(NotificationType.INVOICE_LINKED, request);

            log.info("handleInvoiceLinked() - END - orderId={}, invoiceId={}, recipientEmail={}", orderId, invoiceId, request.getReceiveEmail());

        } catch (Exception ex) {
            log.error("handleInvoiceLinked() - ERROR - Error when sending invoice linked notification - orderId={}, invoiceId={}, error: {}", orderId, invoiceId, ex.getMessage(), ex);
        }
    }

    private CompanyEntity resolveCompanyName(Long companyId) {
        return companyRepository.findById(companyId)
                .orElse(CompanyEntity.builder().id(companyId).build());
    }

    private NotificationRequest buildNotificationRequest(CompanyEntity companyEntity, PartnerEntity partnerEntity, OrderEntity orderEntity, InvoiceEntity invoiceEntity) {

        List<Attachment> attachments = buildAttachments(invoiceEntity);

        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail(StringUtils.defaultIfBlank(partnerEntity.getBillingEmail(), partnerEntity.getEmail()));
        request.setCompany(buildCompany(companyEntity, orderEntity));
        request.setCustomer(buildCustomer(partnerEntity));
        request.setPayload(buildPayload(orderEntity, invoiceEntity));
        request.setAttachment(attachments);

        return request;
    }

    private Company buildCompany(CompanyEntity companyEntity, OrderEntity orderEntity) {
        Company company = new Company();
        company.setId(orderEntity.getCompanyId());
        company.setName(StringUtils.defaultIfBlank(companyEntity.getTradeName(), companyEntity.getCorporateName()));
        return company;
    }

    private Customer buildCustomer(PartnerEntity partnerEntity) {
        Customer customer = new Customer();
        customer.setName(partnerEntity.getName());
        return customer;
    }

    private Payload buildPayload(OrderEntity orderEntity, InvoiceEntity invoiceEntity) {
        Payload payload = new Payload();
        payload.setOrderId(orderEntity.getId());
        payload.setInvoiceNumber(resolveInvoiceNumber(invoiceEntity));
        payload.setDocumentType(invoiceEntity.getDocumentType() != null ? invoiceEntity.getDocumentType().name() : null);
        BigDecimal totalAmount = invoiceEntity.getTotalAmount() != null ? invoiceEntity.getTotalAmount() : orderEntity.getTotalAmount();
        payload.setAmount(totalAmount);
        return payload;
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
