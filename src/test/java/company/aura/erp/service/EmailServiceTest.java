package company.aura.erp.service;

import company.aura.erp.modules.email.domain.Customer;
import company.aura.erp.modules.email.domain.NotificationRequest;
import company.aura.erp.modules.email.domain.Payload;
import company.aura.erp.modules.email.domain.template.EmailTemplate;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.modules.email.exception.NotificationException;
import company.aura.erp.modules.email.service.AmazonSesService;
import company.aura.erp.modules.email.service.EmailService;
import company.aura.erp.modules.email.service.TemplateResolverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private AmazonSesService amazonSesService;

    @Mock
    private TemplateResolverService templateResolverService;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("Não deve tentar enviar se a requisição de notificação for nula")
    void shouldNotSendIfRequestIsNull() {
        emailService.sendNotification(NotificationType.FORGOT_PASSWORD, null);

        verifyNoInteractions(templateResolverService);
        verifyNoInteractions(amazonSesService);
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
