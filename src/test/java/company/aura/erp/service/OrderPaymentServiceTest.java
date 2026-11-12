package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.repository.OrderInstallmentRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {

    @Mock
    private OrderInstallmentRepository orderInstallmentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private OrderPaymentService orderPaymentService;

    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = OrderEntity.builder()
                .id(100L)
                .companyId(10L)
                .partnerId(5L)
                .status(OrderStatus.OPEN)
                .totalAmount(BigDecimal.valueOf(135.00))
                .build();
    }

    @Test
    @DisplayName("Deve processar parcelas com sucesso quando a soma bater exatamente com o total do pedido")
    void shouldProcessInstallmentsSuccessfully() {
        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new OrderInstallmentRequest(
                        1, LocalDate.now(), BigDecimal.valueOf(100.00),
                        PaymentMethod.PIX, true, null, null, null, null, null
                ),
                new OrderInstallmentRequest(
                        2, LocalDate.now().plusDays(30), BigDecimal.valueOf(35.00),
                        PaymentMethod.CREDIT_CARD, false, "VISA", "123456", null, null, null
                )
        ));

        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> {
            OrderInstallmentEntity entity = inv.getArgument(0);
            entity.setId(500L);
            return entity;
        });

        List<OrderInstallmentEntity> result = orderPaymentService.processInstallments(request, sampleOrder);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), result.get(0).getPendingAmount());
        assertEquals(BigDecimal.valueOf(35.00).setScale(2, RoundingMode.HALF_UP), result.get(1).getPendingAmount());
        verify(orderInstallmentRepository, times(2)).save(any(OrderInstallmentEntity.class));
        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransactionEntity.class));
    }

    @Test
    @DisplayName("Deve lançar HTTP 422 quando soma das parcelas divergir do total_final do pedido")
    void shouldThrow422WhenInstallmentsTotalMismatch() {
        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new OrderInstallmentRequest(
                        1, LocalDate.now(), BigDecimal.valueOf(100.00),
                        PaymentMethod.PIX, true, null, null, null, null, null
                )
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.processInstallments(request, sampleOrder)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("100.00"));
        assertTrue(ex.getReason().contains("135.00"));
        verify(orderInstallmentRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("findInstallmentResponses — Deve retornar parcelas com suas transações e saldo calculado")
    void shouldFindInstallmentResponsesWithTransactions() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .dueDate(LocalDate.now())
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(60.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(InstallmentStatus.PARTIALLY_PAID)
                .build();

        PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                .id(10L)
                .installmentId(1L)
                .companyId(10L)
                .amountPaid(BigDecimal.valueOf(40.00))
                .paymentMethod(PaymentMethod.PIX)
                .paidAt(LocalDateTime.now())
                .build();

        when(orderInstallmentRepository.findByOrderId(100L)).thenReturn(List.of(installment));
        when(paymentTransactionRepository.findByInstallmentId(1L)).thenReturn(List.of(tx));

        List<OrderInstallmentResponse> responses = orderPaymentService.findInstallmentResponses(100L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        OrderInstallmentResponse resp = responses.get(0);
        assertEquals(BigDecimal.valueOf(40.00), resp.getPaidAmount());
        assertEquals(BigDecimal.valueOf(60.00), resp.getRemainingAmount());
        assertEquals(BigDecimal.valueOf(60.00), resp.getPendingAmount());
        assertEquals(1, resp.getTransactions().size());
    }

    @Test
    @DisplayName("payInstallment — Deve registrar pagamento parcial e marcar status PARTIALLY_PAID")
    void shouldPayInstallmentPartially() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(100.00))
                .status(InstallmentStatus.PENDING)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(40.00), PaymentMethod.PIX, null, null, null, "Primeira parte"
        );

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));
        when(paymentTransactionRepository.findByInstallmentId(1L)).thenReturn(Collections.emptyList());
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> {
            PaymentTransactionEntity t = inv.getArgument(0);
            t.setId(20L);
            return t;
        });
        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderInstallmentResponse response = orderPaymentService.payInstallment(sampleOrder, 1L, request);

        assertNotNull(response);
        assertEquals(InstallmentStatus.PARTIALLY_PAID, response.getStatus());
        assertEquals(BigDecimal.valueOf(40.00), response.getPaidAmount());
        assertEquals(BigDecimal.valueOf(60.00).setScale(2, RoundingMode.HALF_UP), response.getRemainingAmount());
        assertEquals(BigDecimal.valueOf(60.00).setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        assertEquals(1, response.getTransactions().size());
        verify(paymentTransactionRepository).save(any(PaymentTransactionEntity.class));
    }

    @Test
    @DisplayName("payInstallment — Deve registrar pagamento total e marcar status PAID")
    void shouldPayInstallmentFully() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(60.00))
                .status(InstallmentStatus.PARTIALLY_PAID)
                .build();

        PaymentTransactionEntity prevTx = PaymentTransactionEntity.builder()
                .id(10L)
                .installmentId(1L)
                .amountPaid(BigDecimal.valueOf(40.00))
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(60.00), PaymentMethod.CASH, null, null, null, "Quitação"
        );

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));
        when(paymentTransactionRepository.findByInstallmentId(1L)).thenReturn(List.of(prevTx));
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderInstallmentResponse response = orderPaymentService.payInstallment(sampleOrder, 1L, request);

        assertNotNull(response);
        assertEquals(InstallmentStatus.PAID, response.getStatus());
        assertEquals(BigDecimal.valueOf(100.00), response.getPaidAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRemainingAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getPendingAmount()));
        assertEquals(2, response.getTransactions().size());
    }

    @Test
    @DisplayName("payInstallment — Deve lançar UNPROCESSABLE_ENTITY se valor pago exceder saldo restante")
    void shouldThrow422WhenPaymentExceedsRemainingBalance() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(100.00))
                .status(InstallmentStatus.PENDING)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(150.00), PaymentMethod.PIX, null, null, null, null
        );

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));
        when(paymentTransactionRepository.findByInstallmentId(1L)).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.payInstallment(sampleOrder, 1L, request)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("excede o saldo restante"));
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("payInstallment — Deve lançar UNPROCESSABLE_ENTITY se parcela estiver cancelada")
    void shouldThrow422WhenPayingCanceledInstallment() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.ZERO)
                .status(InstallmentStatus.CANCELED)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(50.00), PaymentMethod.PIX, null, null, null, null
        );

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.payInstallment(sampleOrder, 1L, request)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Não é possível registrar pagamento para uma parcela cancelada."));
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addInstallment — Deve adicionar nova parcela não paga e auto-incrementar installmentNumber")
    void shouldAddNewUnpaidInstallment() {
        OrderInstallmentEntity existing = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .installmentNumber(1)
                .build();

        OrderInstallmentCreateRequest request = new OrderInstallmentCreateRequest(
                null, LocalDate.now().plusDays(15), BigDecimal.valueOf(50.00),
                PaymentMethod.BOLETO, false, null, null, "123456", "http://boleto", "Obs"
        );

        when(orderInstallmentRepository.findByOrderId(100L)).thenReturn(List.of(existing));
        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> {
            OrderInstallmentEntity e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        OrderInstallmentResponse response = orderPaymentService.addInstallment(sampleOrder, request);

        assertNotNull(response);
        assertEquals(2, response.getInstallmentNumber());
        assertEquals(InstallmentStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addInstallment — Deve adicionar nova parcela já paga criando transação inicial")
    void shouldAddNewPaidInstallment() {
        OrderInstallmentCreateRequest request = new OrderInstallmentCreateRequest(
                1, LocalDate.now(), BigDecimal.valueOf(80.00),
                PaymentMethod.CREDIT_CARD, true, "MASTERCARD", "AUTH999", null, null, null
        );

        when(orderInstallmentRepository.findByOrderId(100L)).thenReturn(Collections.emptyList());
        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> {
            OrderInstallmentEntity e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> {
            PaymentTransactionEntity t = inv.getArgument(0);
            t.setId(30L);
            return t;
        });

        OrderInstallmentResponse response = orderPaymentService.addInstallment(sampleOrder, request);

        assertNotNull(response);
        assertEquals(InstallmentStatus.PAID, response.getStatus());
        assertEquals(BigDecimal.valueOf(80.00), response.getPaidAmount());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        assertEquals(1, response.getTransactions().size());
        verify(paymentTransactionRepository).save(any(PaymentTransactionEntity.class));
    }

    @Test
    @DisplayName("cancelInstallment — Deve cancelar parcela não paga com sucesso")
    void shouldCancelUnpaidInstallmentSuccessfully() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .status(InstallmentStatus.PENDING)
                .amount(BigDecimal.valueOf(50.00))
                .pendingAmount(BigDecimal.valueOf(50.00))
                .build();

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));
        when(paymentTransactionRepository.findByInstallmentId(1L)).thenReturn(Collections.emptyList());
        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderInstallmentResponse response = orderPaymentService.cancelInstallment(sampleOrder, 1L);

        assertNotNull(response);
        assertEquals(InstallmentStatus.CANCELED, response.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        verify(orderInstallmentRepository).save(installment);
    }

    @Test
    @DisplayName("cancelInstallment — Deve lançar UNPROCESSABLE_ENTITY se parcela já estiver paga")
    void shouldThrow422WhenCancelingPaidInstallment() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .status(InstallmentStatus.PAID)
                .pendingAmount(BigDecimal.ZERO)
                .build();

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.cancelInstallment(sampleOrder, 1L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(orderInstallmentRepository, never()).save(any());
    }
}
