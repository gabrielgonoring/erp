package company.aura.erp.service;

import company.aura.erp.modules.email.domain.DownloadedAttachment;
import company.aura.erp.modules.email.domain.template.Attachment;
import company.aura.erp.modules.email.domain.template.Company;
import company.aura.erp.modules.email.domain.template.Customer;
import company.aura.erp.modules.email.domain.template.NotificationRequest;
import company.aura.erp.modules.email.domain.template.Payload;
import company.aura.erp.modules.email.domain.template.EmailTemplate;
import company.aura.erp.modules.email.enumerate.AttachmentSource;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.modules.email.exception.NotificationException;
import company.aura.erp.modules.email.service.AmazonSesService;
import company.aura.erp.modules.email.service.AttachmentService;
import company.aura.erp.modules.email.service.EmailService;
import company.aura.erp.modules.email.service.TemplateResolverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private AmazonSesService amazonSesService;

    @Mock
    private TemplateResolverService templateResolverService;

    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("Não deve tentar enviar se a requisição de notificação for nula")
    void shouldNotSendIfRequestIsNull() {
        emailService.sendNotification(NotificationType.FORGOT_PASSWORD, null);

        verifyNoInteractions(templateResolverService);
        verifyNoInteractions(amazonSesService);
        verifyNoInteractions(attachmentService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Não deve tentar enviar se o e-mail de destino for nulo, vazio ou em branco")
    void shouldNotSendIfReceiveEmailIsInvalid(String email) {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail(email);

        emailService.sendNotification(NotificationType.FORGOT_PASSWORD, request);

        verifyNoInteractions(templateResolverService);
        verifyNoInteractions(amazonSesService);
        verifyNoInteractions(attachmentService);
    }

    @Test
    @DisplayName("Deve enviar notificação de recuperação de senha com sucesso")
    void shouldSendForgotPasswordNotificationSuccessfully() {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail("usuario@aura.company");

        Customer customer = new Customer();
        customer.setName("Gabriel Borges");
        request.setCustomer(customer);

        Payload payload = new Payload();
        payload.setResetPasswordUrl("https://erp.aura.company/redefinir-senha?token=abc-123");
        request.setPayload(payload);

        EmailTemplate emailTemplate = EmailTemplate.builder()
                .subject("Redefinição de Senha - ${customerName}")
                .html("<p>Olá ${customerName}, redefina em: ${changePasswordUrl}</p>")
                .build();

        String textMessage = "Olá ${customerName}, redefina em: ${changePasswordUrl}";

        when(templateResolverService.getEmailTemplate(NotificationType.FORGOT_PASSWORD)).thenReturn(emailTemplate);
        when(templateResolverService.getTextMessage(NotificationType.FORGOT_PASSWORD)).thenReturn(textMessage);
        when(amazonSesService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn("ses-msg-123");

        emailService.sendNotification(NotificationType.FORGOT_PASSWORD, request);

        verify(templateResolverService, times(1)).getEmailTemplate(NotificationType.FORGOT_PASSWORD);
        verify(templateResolverService, times(1)).getTextMessage(NotificationType.FORGOT_PASSWORD);
        verify(amazonSesService, times(1)).sendEmail(
                eq("usuario@aura.company"),
                eq("Redefinição de Senha - Gabriel Borges"),
                eq("<p>Olá Gabriel Borges, redefina em: https://erp.aura.company/redefinir-senha?token=abc-123</p>"),
                eq("Olá Gabriel Borges, redefina em: https://erp.aura.company/redefinir-senha?token=abc-123")
        );
    }

    @Test
    @DisplayName("Deve enviar notificação de vencimento de parcela com substituição correta de variáveis")
    void shouldSendInstallmentNotificationSuccessfully() {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail("financeiro@cliente.com");

        Customer customer = new Customer();
        customer.setName("Aura Sistemas");
        request.setCustomer(customer);

        Payload payload = new Payload();
        payload.setOrderId(456L);
        payload.setInstallmentNumber(1);
        payload.setAmount(new BigDecimal("250.75"));
        payload.setInstallmentDueDate(LocalDate.of(2026, 12, 15));
        payload.setPaymentMethod("BOLETO");
        request.setPayload(payload);

        EmailTemplate emailTemplate = EmailTemplate.builder()
                .subject("Parcela ${installmentNumber} - Vencimento ${installmentDueDate}")
                .html("<p>Prezado ${customerName}, parcela ${installmentNumber} no valor de R$ ${amount} vence em ${installmentDueDate}. Pagamento: ${paymentMethod}</p>")
                .build();

        String textMessage = "Prezado ${customerName}, parcela ${installmentNumber} no valor de R$ ${amount} vence em ${installmentDueDate}. Pagamento: ${paymentMethod}";

        when(templateResolverService.getEmailTemplate(NotificationType.INSTALLMENT_NOTIFY)).thenReturn(emailTemplate);
        when(templateResolverService.getTextMessage(NotificationType.INSTALLMENT_NOTIFY)).thenReturn(textMessage);
        when(amazonSesService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn("ses-msg-456");

        emailService.sendNotification(NotificationType.INSTALLMENT_NOTIFY, request);

        verify(templateResolverService, times(1)).getEmailTemplate(NotificationType.INSTALLMENT_NOTIFY);
        verify(templateResolverService, times(1)).getTextMessage(NotificationType.INSTALLMENT_NOTIFY);
        verify(amazonSesService, times(1)).sendEmail(
                eq("financeiro@cliente.com"),
                eq("Parcela 1 - Vencimento 15/12/2026"),
                eq("<p>Prezado Aura Sistemas, parcela 1 no valor de R$ 250,75 vence em 15/12/2026. Pagamento: BOLETO</p>"),
                eq("Prezado Aura Sistemas, parcela 1 no valor de R$ 250,75 vence em 15/12/2026. Pagamento: BOLETO")
        );
    }

    @Test
    @DisplayName("Deve enviar notificação de nota fiscal vinculada (INVOICE_LINKED) com substituição de variáveis e anexos")
    void shouldSendInvoiceLinkedNotificationSuccessfully() {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail("fiscal@cliente.com");

        Company company = new Company();
        company.setId(10L);
        company.setName("Aura Sistemas");
        request.setCompany(company);

        Customer customer = new Customer();
        customer.setName("Cliente LTDA");
        request.setCustomer(customer);

        Payload payload = new Payload();
        payload.setOrderId(100L);
        payload.setInvoiceId(555L);
        payload.setInvoiceNumber("9876");
        payload.setDocumentType("NFE");
        payload.setAmount(new BigDecimal("1250.90"));
        request.setPayload(payload);

        Attachment pdfAtt = Attachment.builder().source(AttachmentSource.BUCKET).url("https://pub-r2.com/nfe.pdf").build();
        Attachment xmlAtt = Attachment.builder().source(AttachmentSource.BUCKET).url("https://pub-r2.com/nfe.xml").build();
        request.setAttachment(List.of(pdfAtt, xmlAtt));

        EmailTemplate emailTemplate = EmailTemplate.builder()
                .subject("Nota Fiscal Disponível - Pedido #${orderId}")
                .html("<p>Olá ${customerName}, ${companyName} vinculou ${documentType} ${invoiceNumber} (#${invoiceId}) de R$ ${amount}</p>")
                .build();

        String textMessage = "Olá ${customerName}, ${companyName} vinculou ${documentType} ${invoiceNumber} (#${invoiceId}) de R$ ${amount}";

        DownloadedAttachment pdfDownloaded = DownloadedAttachment.builder()
                .fileName("nfe.pdf")
                .contentType("application/pdf")
                .data("pdf".getBytes())
                .build();
        DownloadedAttachment xmlDownloaded = DownloadedAttachment.builder()
                .fileName("nfe.xml")
                .contentType("application/xml")
                .data("<xml/>".getBytes())
                .build();

        when(templateResolverService.getEmailTemplate(NotificationType.INVOICE_LINKED)).thenReturn(emailTemplate);
        when(templateResolverService.getTextMessage(NotificationType.INVOICE_LINKED)).thenReturn(textMessage);
        when(attachmentService.downloadAttachments(request.getAttachment())).thenReturn(List.of(pdfDownloaded, xmlDownloaded));
        when(amazonSesService.sendEmail(anyString(), anyString(), anyString(), anyString(), anyList())).thenReturn("ses-msg-inv-1");

        emailService.sendNotification(NotificationType.INVOICE_LINKED, request);

        verify(amazonSesService, times(1)).sendEmail(
                eq("fiscal@cliente.com"),
                eq("Nota Fiscal Disponível - Pedido #100"),
                eq("<p>Olá Cliente LTDA, Aura Sistemas vinculou NFE 9876 (#555) de R$ 1250,90</p>"),
                eq("Olá Cliente LTDA, Aura Sistemas vinculou NFE 9876 (#555) de R$ 1250,90"),
                eq(List.of(pdfDownloaded, xmlDownloaded))
        );
    }

    @Test
    @DisplayName("Deve baixar anexo com sucesso e enviar e-mail com a lista de anexos baixados")
    void shouldDownloadAttachmentAndSendEmailSuccessfully() {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail("cliente@dominio.com");

        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url("https://pub-r2.com/orders/d83f4b82-7472-4d2c-8134-2e92cbbd42b1-boleto.pdf")
                .build();
        request.setAttachment(List.of(attachment));

        EmailTemplate emailTemplate = EmailTemplate.builder()
                .subject("Cobrança")
                .html("<p>Segue seu boleto</p>")
                .build();

        when(templateResolverService.getEmailTemplate(NotificationType.INSTALLMENT_NOTIFY)).thenReturn(emailTemplate);
        when(templateResolverService.getTextMessage(NotificationType.INSTALLMENT_NOTIFY)).thenReturn("Segue seu boleto");

        byte[] pdfBytes = "PDF content simulated".getBytes();
        DownloadedAttachment downloadedAttachment = DownloadedAttachment.builder()
                .fileName("boleto.pdf")
                .contentType("application/pdf")
                .data(pdfBytes)
                .build();

        when(attachmentService.downloadAttachments(request.getAttachment()))
                .thenReturn(List.of(downloadedAttachment));
        when(amazonSesService.sendEmail(anyString(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn("ses-msg-att-123");

        emailService.sendNotification(NotificationType.INSTALLMENT_NOTIFY, request);

        verify(attachmentService, times(1)).downloadAttachments(request.getAttachment());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DownloadedAttachment>> captor = ArgumentCaptor.forClass(List.class);
        verify(amazonSesService, times(1)).sendEmail(
                eq("cliente@dominio.com"),
                eq("Cobrança"),
                eq("<p>Segue seu boleto</p>"),
                eq("Segue seu boleto"),
                captor.capture()
        );

        List<DownloadedAttachment> capturedAttachments = captor.getValue();
        assertEquals(1, capturedAttachments.size());
        DownloadedAttachment captured = capturedAttachments.get(0);
        assertEquals("boleto.pdf", captured.getFileName());
        assertEquals("application/pdf", captured.getContentType());
        assertArrayEquals(pdfBytes, captured.getData());
    }

    @Test
    @DisplayName("Deve enviar e-mail sem anexo caso a lista de anexos baixados seja vazia")
    void shouldSendEmailWithoutAttachmentWhenDownloadedAttachmentsIsEmpty() {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail("cliente@dominio.com");

        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url("https://pub-r2.com/orders/falha.pdf")
                .build();
        request.setAttachment(List.of(attachment));

        EmailTemplate emailTemplate = EmailTemplate.builder()
                .subject("Cobrança")
                .html("<p>Segue seu boleto</p>")
                .build();

        when(templateResolverService.getEmailTemplate(NotificationType.INSTALLMENT_NOTIFY)).thenReturn(emailTemplate);
        when(templateResolverService.getTextMessage(NotificationType.INSTALLMENT_NOTIFY)).thenReturn("Segue seu boleto");
        when(attachmentService.downloadAttachments(request.getAttachment())).thenReturn(Collections.emptyList());
        when(amazonSesService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn("ses-msg-fallback-456");

        assertDoesNotThrow(() -> emailService.sendNotification(NotificationType.INSTALLMENT_NOTIFY, request));

        verify(attachmentService, times(1)).downloadAttachments(request.getAttachment());
        verify(amazonSesService, times(1)).sendEmail(
                eq("cliente@dominio.com"),
                eq("Cobrança"),
                eq("<p>Segue seu boleto</p>"),
                eq("Segue seu boleto")
        );
        verify(amazonSesService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Deve enviar e-mail sem anexo caso o serviço retorne lista nula de anexos baixados")
    void shouldSendEmailWithoutAttachmentWhenDownloadedAttachmentsIsNull() {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail("cliente@dominio.com");

        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url("https://pub-r2.com/orders/vazio.pdf")
                .build();
        request.setAttachment(List.of(attachment));

        EmailTemplate emailTemplate = EmailTemplate.builder()
                .subject("Cobrança")
                .html("<p>Segue seu boleto</p>")
                .build();

        when(templateResolverService.getEmailTemplate(NotificationType.INSTALLMENT_NOTIFY)).thenReturn(emailTemplate);
        when(templateResolverService.getTextMessage(NotificationType.INSTALLMENT_NOTIFY)).thenReturn("Segue seu boleto");
        when(attachmentService.downloadAttachments(request.getAttachment())).thenReturn(null);
        when(amazonSesService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn("ses-msg-vazio");

        emailService.sendNotification(NotificationType.INSTALLMENT_NOTIFY, request);

        verify(attachmentService, times(1)).downloadAttachments(request.getAttachment());
        verify(amazonSesService, times(1)).sendEmail(
                eq("cliente@dominio.com"),
                eq("Cobrança"),
                eq("<p>Segue seu boleto</p>"),
                eq("Segue seu boleto")
        );
        verify(amazonSesService, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Deve propagar exceção quando o serviço de template lançar erro")
    void shouldPropagateExceptionWhenTemplateResolverFails() {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail("usuario@aura.company");

        when(templateResolverService.getEmailTemplate(NotificationType.FORGOT_PASSWORD))
                .thenThrow(new NotificationException("Template não encontrado"));

        NotificationException ex = assertThrows(NotificationException.class, () ->
                emailService.sendNotification(NotificationType.FORGOT_PASSWORD, request)
        );

        assertEquals("Template não encontrado", ex.getMessage());
        verifyNoInteractions(amazonSesService);
        verifyNoInteractions(attachmentService);
    }

    @Test
    @DisplayName("Deve propagar exceção quando o Amazon SES falhar no envio")
    void shouldPropagateExceptionWhenAmazonSesFails() {
        NotificationRequest request = new NotificationRequest();
        request.setReceiveEmail("usuario@aura.company");

        EmailTemplate emailTemplate = EmailTemplate.builder()
                .subject("Assunto")
                .html("<p>HTML</p>")
                .build();

        when(templateResolverService.getEmailTemplate(NotificationType.FORGOT_PASSWORD)).thenReturn(emailTemplate);
        when(templateResolverService.getTextMessage(NotificationType.FORGOT_PASSWORD)).thenReturn("Texto");
        when(amazonSesService.sendEmail(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Falha de comunicação com AWS SES"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                emailService.sendNotification(NotificationType.FORGOT_PASSWORD, request)
        );

        assertEquals("Falha de comunicação com AWS SES", ex.getMessage());
        verify(amazonSesService, times(1)).sendEmail(anyString(), anyString(), anyString(), anyString());
    }
}
