package company.aura.erp.service;

import company.aura.erp.modules.bucket.service.CloudflareR2Service;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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

    @Mock
    private CloudflareR2Service r2Service;

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
        assertEquals(0, BigDecimal.valueOf(40.00).compareTo(response.getPaidAmount()));
        assertEquals(0, BigDecimal.valueOf(60.00).compareTo(response.getRemainingAmount()));
        assertEquals(0, BigDecimal.valueOf(60.00).compareTo(response.getPendingAmount()));
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
                BigDecimal.valueOf(60.00), PaymentMethod.CREDIT_CARD, null, null, null, "Quitação"
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
    @DisplayName("payInstallment — Deve lançar UNPROCESSABLE_ENTITY se valor exceder saldo")
    void shouldThrow422WhenPaymentExceedsRemaining() {
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
        OrderInstallmentCreateRequest request = new OrderInstallmentCreateRequest(
                null, LocalDate.now().plusDays(15), BigDecimal.valueOf(50.00),
                PaymentMethod.BOLETO, false, null, null, "123456", "http://boleto", "Obs"
        );

        when(orderInstallmentRepository.findMaxInstallmentNumberByOrderId(100L)).thenReturn(1);
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

    @Test
    @DisplayName("uploadBankSlip — Deve fazer upload do boleto com sucesso e atualizar a parcela")
    void shouldUploadBankSlipSuccessfully() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .status(InstallmentStatus.PENDING)
                .amount(BigDecimal.valueOf(50.00))
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "boleto.pdf", "application/pdf", "content".getBytes());

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));
        when(r2Service.upload(eq(file), eq("bank-slips/10/100"))).thenReturn("key-123");
        when(r2Service.getFileUrl("key-123")).thenReturn("http://r2/boleto.pdf");
        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findByInstallmentId(1L)).thenReturn(Collections.emptyList());

        OrderInstallmentResponse response = orderPaymentService.uploadBankSlip(sampleOrder, 1L, file, "CODE-999");

        assertNotNull(response);
        assertEquals("http://r2/boleto.pdf", response.getBankSlipUrl());
        assertEquals("CODE-999", response.getBankSlipCode());
        verify(r2Service).upload(file, "bank-slips/10/100");
        verify(orderInstallmentRepository).save(installment);
    }

    @Test
    @DisplayName("uploadBankSlip — Deve lançar BAD_REQUEST quando arquivo for nulo ou vazio")
    void shouldThrowBadRequestWhenFileIsEmpty() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .status(InstallmentStatus.PENDING)
                .build();

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "application/pdf", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.uploadBankSlip(sampleOrder, 1L, emptyFile, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(r2Service, never()).upload(any(), any());
    }

    @Test
    @DisplayName("uploadBankSlip — Deve lançar UNPROCESSABLE_ENTITY se parcela estiver cancelada")
    void shouldThrowUnprocessableEntityWhenUploadingToCanceledInstallment() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .status(InstallmentStatus.CANCELED)
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "boleto.pdf", "application/pdf", "content".getBytes());
        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.uploadBankSlip(sampleOrder, 1L, file, null)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(r2Service, never()).upload(any(), any());
    }

    @Test
    @DisplayName("getBankSlipUrl — Deve gerar url temporária para download com sucesso")
    void shouldGetBankSlipUrlSuccessfully() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .bankSlipUrl("http://r2/boleto.pdf")
                .build();

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));
        when(r2Service.generateTemporaryUrl(eq("http://r2/boleto.pdf"), eq(Duration.ofMinutes(5))))
                .thenReturn("http://r2/presigned-url");

        String url = orderPaymentService.getBankSlipUrl(sampleOrder, 1L);

        assertEquals("http://r2/presigned-url", url);
    }

    @Test
    @DisplayName("getBankSlipUrl — Deve lançar NOT_FOUND quando parcela não possuir arquivo")
    void shouldThrowNotFoundWhenInstallmentHasNoBankSlipUrl() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .bankSlipUrl(null)
                .build();

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.getBankSlipUrl(sampleOrder, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteBankSlip — Deve excluir boleto com sucesso e limpar os campos da parcela")
    void shouldDeleteBankSlipSuccessfully() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .bankSlipUrl("http://r2/boleto.pdf")
                .bankSlipCode("CODE-999")
                .build();

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));
        when(r2Service.extractKeyFromUrl("http://r2/boleto.pdf")).thenReturn("key-123");

        orderPaymentService.deleteBankSlip(sampleOrder, 1L);

        assertNull(installment.getBankSlipUrl());
        assertNull(installment.getBankSlipCode());
        verify(r2Service).delete("key-123");
        verify(orderInstallmentRepository).save(installment);
    }

    @Test
    @DisplayName("deleteBankSlip — Deve lançar NOT_FOUND quando parcela não possuir boleto")
    void shouldThrowNotFoundWhenDeletingNonExistentBankSlip() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .bankSlipUrl(null)
                .build();

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.deleteBankSlip(sampleOrder, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(r2Service, never()).delete(any());
    }

    @Test
    @DisplayName("deleteBankSlip — Deve lançar UNPROCESSABLE_ENTITY se parcela estiver cancelada")
    void shouldThrowUnprocessableEntityWhenDeletingFromCanceledInstallment() {
        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .status(InstallmentStatus.CANCELED)
                .bankSlipUrl("http://r2/boleto.pdf")
                .build();

        when(orderInstallmentRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(installment));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.deleteBankSlip(sampleOrder, 1L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(r2Service, never()).delete(any());
    }

    @Test
    @DisplayName("listInstallments — Deve listar parcelas sem filtros opcionais com sucesso")
    void shouldListInstallmentsSuccessfully() {
        Pageable pageable = PageRequest.of(0, 20);
        OrderInstallmentSummaryResponse summary = new OrderInstallmentSummaryResponse(
                1L, 100L, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00), InstallmentStatus.PENDING, PaymentMethod.BOLETO, LocalDate.of(2026, 12, 1)
        );
        Page<OrderInstallmentSummaryResponse> expectedPage = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderInstallmentRepository.searchSummarized(
                eq(10L), isNull(), eq(false), eq(false), eq(false), isNull(), isNull(), eq(false), anyList(), eq(pageable)
        )).thenReturn(expectedPage);

        Page<OrderInstallmentSummaryResponse> result = orderPaymentService.listInstallments(
                null, null, null, null, 10L, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(summary, result.getContent().get(0));
        verify(orderInstallmentRepository).searchSummarized(
                eq(10L), isNull(), eq(false), eq(false), eq(false), isNull(), isNull(), eq(false), anyList(), eq(pageable)
        );
    }

    @Test
    @DisplayName("listInstallments — Deve listar parcelas com filtros opcionais de busca, data e status")
    void shouldListInstallmentsWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<InstallmentStatus> statuses = List.of(InstallmentStatus.PENDING, InstallmentStatus.PARTIALLY_PAID);

        OrderInstallmentSummaryResponse summary = new OrderInstallmentSummaryResponse(
                1L, 100L, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00), InstallmentStatus.PARTIALLY_PAID, PaymentMethod.PIX, LocalDate.of(2026, 11, 15)
        );
        Page<OrderInstallmentSummaryResponse> expectedPage = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderInstallmentRepository.searchSummarized(
                eq(10L), eq("Cliente"), eq(true), eq(false), eq(false), eq(startDate), eq(endDate), eq(true), eq(statuses), eq(pageable)
        )).thenReturn(expectedPage);

        Page<OrderInstallmentSummaryResponse> result = orderPaymentService.listInstallments(
                "  Cliente  ", startDate, endDate, statuses, 10L, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(summary, result.getContent().get(0));
        verify(orderInstallmentRepository).searchSummarized(
                eq(10L), eq("Cliente"), eq(true), eq(false), eq(false), eq(startDate), eq(endDate), eq(true), eq(statuses), eq(pageable)
        );
    }

    @Test
    @DisplayName("listInstallments — Deve lançar BAD_REQUEST quando startDate for posterior a endDate")
    void shouldThrowBadRequestWhenStartDateAfterEndDateInListInstallments() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 12, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.listInstallments(null, startDate, endDate, null, 10L, pageable)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderInstallmentRepository, never()).searchSummarized(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any(), anyBoolean(), any(), any());
    }
}
