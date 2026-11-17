package company.aura.erp.service;

import company.aura.erp.dto.job.OrderInstallmentDueNotice;
import company.aura.erp.email.domain.NotificationRequest;
import company.aura.erp.email.enumerate.NotificationType;
import company.aura.erp.email.service.EmailService;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.repository.OrderInstallmentRepository;
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
    private OrderInstallmentRepository orderInstallmentRepository;

    @Mock
    private EmailService emailService;

    @Spy
    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @InjectMocks
    private JobService jobService;

    @Test
    @DisplayName("notifyUpcomingInstallments — Deve recuperar parcelas paginadas a vencer em 2 dias e enviar e-mails")
    void shouldNotifyUpcomingInstallmentsSuccessfully() {
        LocalDate targetDueDate = LocalDate.now().plusDays(2);

        OrderInstallmentDueNotice notice1 = new OrderInstallmentDueNotice(
                1L, 100L, 1, BigDecimal.valueOf(250.00), BigDecimal.valueOf(250.00),
                targetDueDate, PaymentMethod.BOLETO, "http://boleto.pdf", "1234567890",
                10L, "Cliente Um", "cliente1@teste.com", null
        );

        OrderInstallmentDueNotice notice2 = new OrderInstallmentDueNotice(
                2L, 101L, 2, BigDecimal.valueOf(500.00), BigDecimal.valueOf(300.00),
                targetDueDate, PaymentMethod.PIX, null, null,
                10L, "Cliente Dois", "cliente2@teste.com", "financeiro@cliente2.com"
        );

        Page<OrderInstallmentDueNotice> page = new PageImpl<>(List.of(notice1, notice2), PageRequest.of(0, 50), 2);

        when(orderInstallmentRepository.findInstallmentsDueNotice(
                eq(targetDueDate),
                eq(List.of(InstallmentStatus.PAID, InstallmentStatus.CANCELED)),
                any(Pageable.class)
        )).thenReturn(page);

        jobService.notifyUpcomingInstallments();

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
    @DisplayName("notifyUpcomingInstallments — Deve enviar notificação mesmo com e-mail nulo para que EmailService trate")
    void shouldHandleWhenCustomerHasNoEmail() {
        LocalDate targetDueDate = LocalDate.now().plusDays(2);

        OrderInstallmentDueNotice noticeWithoutEmail = new OrderInstallmentDueNotice(
                1L, 100L, 1, BigDecimal.valueOf(250.00), BigDecimal.valueOf(250.00),
                targetDueDate, PaymentMethod.BOLETO, null, null,
                10L, "Cliente Sem Email", null, null
        );

        Page<OrderInstallmentDueNotice> page = new PageImpl<>(List.of(noticeWithoutEmail), PageRequest.of(0, 50), 1);

        when(orderInstallmentRepository.findInstallmentsDueNotice(
                eq(targetDueDate), anyList(), any(Pageable.class)
        )).thenReturn(page);

        assertDoesNotThrow(() -> jobService.notifyUpcomingInstallments());

        verify(emailService, timeout(2000).times(1)).sendNotification(
                eq(NotificationType.INSTALLMENT_NOTIFY),
                any(NotificationRequest.class)
        );
    }

    @Test
    @DisplayName("notifyUpcomingInstallments — Deve tratar falha no envio de e-mail sem abortar execução geral")
    void shouldHandleEmailSendFailureGracefully() {
        LocalDate targetDueDate = LocalDate.now().plusDays(2);

        OrderInstallmentDueNotice notice = new OrderInstallmentDueNotice(
                1L, 100L, 1, BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00),
                targetDueDate, PaymentMethod.PIX, null, null,
                10L, "Cliente Teste", "erro@teste.com", null
        );

        Page<OrderInstallmentDueNotice> page = new PageImpl<>(List.of(notice), PageRequest.of(0, 50), 1);

        when(orderInstallmentRepository.findInstallmentsDueNotice(
                eq(targetDueDate), anyList(), any(Pageable.class)
        )).thenReturn(page);

        doThrow(new RuntimeException("SES Error"))
                .when(emailService)
                .sendNotification(eq(NotificationType.INSTALLMENT_NOTIFY), any(NotificationRequest.class));

        assertDoesNotThrow(() -> jobService.notifyUpcomingInstallments());

        verify(emailService, timeout(2000).times(1)).sendNotification(
                eq(NotificationType.INSTALLMENT_NOTIFY),
                any(NotificationRequest.class)
        );
    }
}
