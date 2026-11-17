package company.aura.erp.service;

import company.aura.erp.entity.CompanyEntity;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.event.InvoiceLinkedEvent;
import company.aura.erp.modules.email.domain.template.NotificationRequest;
import company.aura.erp.modules.email.enumerate.AttachmentSource;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.modules.email.service.EmailService;
import company.aura.erp.repository.CompanyRepository;
import company.aura.erp.repository.InvoiceRepository;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.PartnerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderInvoiceNotificationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderInvoiceNotificationService notificationService;

    @Test
    @DisplayName("handleInvoiceLinked deve estar anotado com @Async e @TransactionalEventListener(AFTER_COMMIT)")
    void shouldHaveAsyncAndTransactionalEventListenerAnnotations() throws NoSuchMethodException {
        Method method = OrderInvoiceNotificationService.class.getMethod("sendInvoiceToCustomer", InvoiceLinkedEvent.class);

        Async asyncAnnotation = method.getAnnotation(Async.class);
        TransactionalEventListener txListener = method.getAnnotation(TransactionalEventListener.class);

        assertNotNull(asyncAnnotation);
        assertNotNull(txListener);
        assertEquals(TransactionPhase.AFTER_COMMIT, txListener.phase());
        assertTrue(txListener.fallbackExecution());
    }

    @Test
    @DisplayName("Deve levantar dados do pedido, cliente, nota e empresa e disparar notificação INVOICE_LINKED com PDF e XML")
    void shouldPrepareAndSendInvoiceLinkedNotificationSuccessfully() {
        Long companyId = 10L;
        Long orderId = 100L;
        Long invoiceId = 555L;
        Long partnerId = 50L;

        OrderEntity order = OrderEntity.builder()
                .id(orderId)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .totalAmount(new BigDecimal("250.00"))
                .build();

        PartnerEntity partner = PartnerEntity.builder()
                .id(partnerId)
                .companyId(companyId)
                .name("Cliente Exemplo LTDA")
                .email("contato@cliente.com")
                .billingEmail("fiscal@cliente.com")
                .build();

        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(invoiceId)
                .companyId(companyId)
                .partnerId(partnerId)
                .documentType(DocumentType.NFE)
                .invoiceNumber(1234)
                .totalAmount(new BigDecimal("275.50"))
                .pdfUrl("https://pub-r2.com/invoices/danfe.pdf")
                .xmlUrl("https://pub-r2.com/invoices/nfe.xml")
                .build();

        CompanyEntity company = CompanyEntity.builder()
                .id(companyId)
                .corporateName("Aura Tecnologia LTDA")
                .tradeName("Aura Sistemas")
                .build();

        when(orderRepository.findByIdAndCompanyId(orderId, companyId)).thenReturn(Optional.of(order));
        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId)).thenReturn(Optional.of(partner));
        when(invoiceRepository.findByIdAndCompanyId(invoiceId, companyId)).thenReturn(Optional.of(invoice));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        notificationService.sendInvoiceToCustomer(new InvoiceLinkedEvent(companyId, orderId, invoiceId));

        ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(emailService, times(1)).sendNotification(eq(NotificationType.INVOICE_LINKED), requestCaptor.capture());

        NotificationRequest sent = requestCaptor.getValue();
        assertEquals("fiscal@cliente.com", sent.getReceiveEmail());
        assertEquals("Cliente Exemplo LTDA", sent.getCustomer().getName());
        assertEquals(companyId, sent.getCompany().getId());
        assertEquals("Aura Sistemas", sent.getCompany().getName());
        assertEquals(orderId, sent.getPayload().getOrderId());
        assertEquals(invoiceId, sent.getPayload().getInvoiceId());
        assertEquals("1234", sent.getPayload().getInvoiceNumber());
        assertEquals("NFE", sent.getPayload().getDocumentType());
        assertEquals(new BigDecimal("275.50"), sent.getPayload().getAmount());

        assertEquals(2, sent.getAttachment().size());
        assertEquals(AttachmentSource.BUCKET, sent.getAttachment().get(0).getSource());
        assertEquals("https://pub-r2.com/invoices/danfe.pdf", sent.getAttachment().get(0).getUrl());
        assertEquals(AttachmentSource.BUCKET, sent.getAttachment().get(1).getSource());
        assertEquals("https://pub-r2.com/invoices/nfe.xml", sent.getAttachment().get(1).getUrl());
    }

    @Test
    @DisplayName("Deve usar email principal do cliente, corporateName da empresa e ID da nota como fallbacks")
    void shouldUseFallbacksWhenOptionalFieldsAreMissing() {
        Long companyId = 10L;
        Long orderId = 100L;
        Long invoiceId = 555L;
        Long partnerId = 50L;

        OrderEntity order = OrderEntity.builder()
                .id(orderId)
                .companyId(companyId)
                .partnerId(partnerId)
                .totalAmount(new BigDecimal("199.90"))
                .build();

        PartnerEntity partner = PartnerEntity.builder()
                .id(partnerId)
                .companyId(companyId)
                .name("João Silva")
                .email("joao@cliente.com")
                .billingEmail("   ")
                .build();

        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(invoiceId)
                .companyId(companyId)
                .partnerId(partnerId)
                .documentType(DocumentType.NFSE)
                .invoiceNumber(null)
                .totalAmount(null)
                .pdfUrl("https://pub-r2.com/invoices/nfse.pdf")
                .xmlUrl(null)
                .build();

        CompanyEntity company = CompanyEntity.builder()
                .id(companyId)
                .corporateName("Razão Social LTDA")
                .tradeName("")
                .build();

        when(orderRepository.findByIdAndCompanyId(orderId, companyId)).thenReturn(Optional.of(order));
        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId)).thenReturn(Optional.of(partner));
        when(invoiceRepository.findByIdAndCompanyId(invoiceId, companyId)).thenReturn(Optional.of(invoice));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        notificationService.sendInvoiceToCustomer(new InvoiceLinkedEvent(companyId, orderId, invoiceId));

        ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(emailService, times(1)).sendNotification(eq(NotificationType.INVOICE_LINKED), requestCaptor.capture());

        NotificationRequest sent = requestCaptor.getValue();
        assertEquals("joao@cliente.com", sent.getReceiveEmail());
        assertEquals("Razão Social LTDA", sent.getCompany().getName());
        assertEquals("555", sent.getPayload().getInvoiceNumber());
        assertEquals("NFSE", sent.getPayload().getDocumentType());
        assertEquals(new BigDecimal("199.90"), sent.getPayload().getAmount());
        assertEquals(1, sent.getAttachment().size());
        assertEquals("https://pub-r2.com/invoices/nfse.pdf", sent.getAttachment().get(0).getUrl());
    }

    @Test
    @DisplayName("Não deve enviar notificação quando cliente não possuir e-mail")
    void shouldNotSendNotificationWhenCustomerHasNoEmail() {
        Long companyId = 10L;
        Long orderId = 100L;
        Long invoiceId = 555L;
        Long partnerId = 50L;

        OrderEntity order = OrderEntity.builder().id(orderId).companyId(companyId).partnerId(partnerId).build();
        PartnerEntity partner = PartnerEntity.builder().id(partnerId).companyId(companyId).name("Sem Email").build();

        when(orderRepository.findByIdAndCompanyId(orderId, companyId)).thenReturn(Optional.of(order));
        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId)).thenReturn(Optional.of(partner));

        notificationService.sendInvoiceToCustomer(new InvoiceLinkedEvent(companyId, orderId, invoiceId));

        verifyNoInteractions(invoiceRepository, companyRepository, emailService);
    }

    @Test
    @DisplayName("Não deve enviar notificação quando evento, pedido, parceiro ou nota não forem encontrados")
    void shouldNotSendNotificationWhenDataMissing() {
        notificationService.sendInvoiceToCustomer(null);
        notificationService.sendInvoiceToCustomer(new InvoiceLinkedEvent(null, 1L, 2L));

        when(orderRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.empty());
        notificationService.sendInvoiceToCustomer(new InvoiceLinkedEvent(10L, 100L, 555L));

        OrderEntity order = OrderEntity.builder().id(100L).companyId(10L).partnerId(50L).build();
        PartnerEntity partner = PartnerEntity.builder().id(50L).companyId(10L).email("c@c.com").build();
        when(orderRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(order));
        when(partnerRepository.findByIdAndCompanyId(50L, 10L)).thenReturn(Optional.of(partner));
        when(invoiceRepository.findByIdAndCompanyId(555L, 10L)).thenReturn(Optional.empty());

        notificationService.sendInvoiceToCustomer(new InvoiceLinkedEvent(10L, 100L, 555L));

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Deve capturar exceções inesperadas no envio assíncrono sem propagar erro")
    void shouldCatchExceptionWithoutPropagating() {
        Long companyId = 10L;
        Long orderId = 100L;
        Long invoiceId = 555L;
        Long partnerId = 50L;

        OrderEntity order = OrderEntity.builder().id(orderId).companyId(companyId).partnerId(partnerId).build();
        PartnerEntity partner = PartnerEntity.builder().id(partnerId).companyId(companyId).name("Cliente").email("c@c.com").build();
        InvoiceEntity invoice = InvoiceEntity.builder().id(invoiceId).companyId(companyId).documentType(DocumentType.NFE).build();

        when(orderRepository.findByIdAndCompanyId(orderId, companyId)).thenReturn(Optional.of(order));
        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId)).thenReturn(Optional.of(partner));
        when(invoiceRepository.findByIdAndCompanyId(invoiceId, companyId)).thenReturn(Optional.of(invoice));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("SES failure")).when(emailService).sendNotification(eq(NotificationType.INVOICE_LINKED), any());

        assertDoesNotThrow(() -> notificationService.sendInvoiceToCustomer(new InvoiceLinkedEvent(companyId, orderId, invoiceId)));
    }
}
