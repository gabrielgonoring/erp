package company.aura.erp.service;

import company.aura.erp.dto.job.ReceivableDueNotice;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.modules.email.domain.template.NotificationRequest;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.modules.email.service.EmailService;
import company.aura.erp.repository.ReceivableRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private EmailService emailService;

    @Spy
    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @InjectMocks
    private JobService jobService;

    @Test
    @DisplayName("notifyUpcomingReceivables — Deve recuperar recebíveis paginados a vencer em 2 dias e enviar e-mails")
    void shouldNotifyUpcomingReceivablesSuccessfully() {
        LocalDate targetDueDate = LocalDate.now().plusDays(2);

        ReceivableDueNotice notice1 = new ReceivableDueNotice(
                1L, 100L, null, 1, BigDecimal.valueOf(250.00), BigDecimal.valueOf(250.00),
                targetDueDate, PaymentMethod.BOLETO, "http://boleto.pdf", "https://pay.aura/1", "1234567890",
                10L, "Cliente Um", "cliente1@teste.com", null
        );

        ReceivableDueNotice notice2 = new ReceivableDueNotice(
                2L, 101L, null, 2, BigDecimal.valueOf(500.00), BigDecimal.valueOf(300.00),
                targetDueDate, PaymentMethod.PIX, null, null, null,
                10L, "Cliente Dois", "cliente2@teste.com", "financeiro@cliente2.com"
        );

        Page<ReceivableDueNotice> page = new PageImpl<>(List.of(notice1, notice2), PageRequest.of(0, 50), 2);

        when(receivableRepository.findReceivablesDueNotice(
                eq(targetDueDate),
                eq(List.of(ReceivableStatus.PAID, ReceivableStatus.CANCELED)),
                any(Pageable.class)
        )).thenReturn(page);

        jobService.notifyUpcomingReceivables();

        ArgumentCaptor<NotificationRequest> requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(emailService, timeout(2000).times(2)).sendNotification(
                eq(NotificationType.INSTALLMENT_NOTIFY),
                requestCaptor.capture()
        );

        List<NotificationRequest> capturedRequests = requestCaptor.getAllValues();
        assertEquals(2, capturedRequests.size());
        List<String> emails = capturedRequests.stream().map(NotificationRequest::getReceiveEmail).toList();
        assertTrue(emails.contains("cliente1@teste.com"));
        assertTrue(emails.contains("financeiro@cliente2.com"));

        List<String> names = capturedRequests.stream().map(r -> r.getCustomer().getName()).toList();
        assertTrue(names.contains("Cliente Um"));
        assertTrue(names.contains("Cliente Dois"));
    }

    @Test
    @DisplayName("notifyUpcomingReceivables — Deve enviar notificação mesmo com e-mail nulo para que EmailService trate")
    void shouldHandleWhenCustomerHasNoEmail() {
        LocalDate targetDueDate = LocalDate.now().plusDays(2);

        ReceivableDueNotice noticeWithoutEmail = new ReceivableDueNotice(
                1L, 100L, null, 1, BigDecimal.valueOf(250.00), BigDecimal.valueOf(250.00),
                targetDueDate, PaymentMethod.BOLETO, null, null, null,
                10L, "Cliente Sem Email", null, null
        );

        Page<ReceivableDueNotice> page = new PageImpl<>(List.of(noticeWithoutEmail), PageRequest.of(0, 50), 1);

        when(receivableRepository.findReceivablesDueNotice(
                eq(targetDueDate), anyList(), any(Pageable.class)
        )).thenReturn(page);

        assertDoesNotThrow(() -> jobService.notifyUpcomingReceivables());

        verify(emailService, timeout(2000).times(1)).sendNotification(
                eq(NotificationType.INSTALLMENT_NOTIFY),
                any(NotificationRequest.class)
        );
    }

    @Test
    @DisplayName("notifyUpcomingReceivables — Deve tratar falha no envio de e-mail sem abortar execução geral")
    void shouldHandleEmailSendFailureGracefully() {
        LocalDate targetDueDate = LocalDate.now().plusDays(2);

        ReceivableDueNotice notice = new ReceivableDueNotice(
                1L, 100L, null, 1, BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00),
                targetDueDate, PaymentMethod.PIX, null, null, null,
                10L, "Cliente Teste", "erro@teste.com", null
        );

        Page<ReceivableDueNotice> page = new PageImpl<>(List.of(notice), PageRequest.of(0, 50), 1);

        when(receivableRepository.findReceivablesDueNotice(
                eq(targetDueDate), anyList(), any(Pageable.class)
        )).thenReturn(page);

        doThrow(new RuntimeException("SES Error"))
                .when(emailService)
                .sendNotification(eq(NotificationType.INSTALLMENT_NOTIFY), any(NotificationRequest.class));

        assertDoesNotThrow(() -> jobService.notifyUpcomingReceivables());

        verify(emailService, timeout(2000).times(1)).sendNotification(
                eq(NotificationType.INSTALLMENT_NOTIFY),
                any(NotificationRequest.class)
        );
    }
}
