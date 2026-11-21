package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.entity.ReceivableEntity;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.PaymentTransactionType;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.modules.bucket.service.CloudflareR2Service;
import company.aura.erp.repository.PaymentTransactionRepository;
import company.aura.erp.repository.ReceivableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ReceivedServiceTest {

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private CloudflareR2Service r2Service;

    @InjectMocks
    private ReceivedService receivedService;

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
    @DisplayName("Deve processar recebíveis com sucesso quando a soma bater exatamente com o total do pedido")
    void shouldProcessReceivablesSuccessfully() {
        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new ReceivableRequest(
                        1, LocalDate.now(), BigDecimal.valueOf(100.00),
                        PaymentMethod.PIX, true, null, null, null, null, "https://pay.aura/pix-1", null
                ),
                new ReceivableRequest(
                        2, LocalDate.now().plusDays(30), BigDecimal.valueOf(35.00),
                        PaymentMethod.CREDIT_CARD, false, "VISA", "123456", null, null, "https://pay.aura/card-2", null
                )
        ));

        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> {
            ReceivableEntity entity = inv.getArgument(0);
            entity.setId(500L);
            return entity;
        });

        List<ReceivableEntity> result = receivedService.processReceivables(request, sampleOrder);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), result.get(0).getPendingAmount());
        assertEquals("https://pay.aura/pix-1", result.get(0).getPaymentUrl());
        assertEquals(BigDecimal.valueOf(35.00).setScale(2, RoundingMode.HALF_UP), result.get(1).getPendingAmount());
        assertEquals("https://pay.aura/card-2", result.get(1).getPaymentUrl());
        verify(receivableRepository, times(2)).save(any(ReceivableEntity.class));

        ArgumentCaptor<PaymentTransactionEntity> txCaptor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionRepository, times(1)).save(txCaptor.capture());
        assertEquals(PaymentTransactionType.PAYMENT, txCaptor.getValue().getTransactionType());
        assertEquals(500L, txCaptor.getValue().getReceivableId());
    }

    @Test
    @DisplayName("Deve lançar HTTP 422 quando soma dos recebíveis divergir do total_final do pedido")
    void shouldThrow422WhenReceivablesTotalMismatch() {
        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new ReceivableRequest(
                        1, LocalDate.now(), BigDecimal.valueOf(100.00),
                        PaymentMethod.PIX, true, null, null, null, null, null, null
                )
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.processReceivables(request, sampleOrder)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("100.00"));
        assertTrue(ex.getReason().contains("135.00"));
        verify(receivableRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("findReceivableResponses — Deve retornar recebíveis com suas transações e saldo calculado")
    void shouldFindReceivableResponsesWithTransactions() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .dueDate(LocalDate.now())
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(60.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(ReceivableStatus.PARTIALLY_PAID)
                .paymentFileUrl("https://r2/file.pdf")
                .paymentUrl("https://pay.aura/1")
                .build();

        PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                .id(10L)
                .receivableId(1L)
                .companyId(10L)
                .transactionType(PaymentTransactionType.PAYMENT)
                .amountPaid(BigDecimal.valueOf(40.00))
                .paymentMethod(PaymentMethod.PIX)
                .paidAt(LocalDateTime.now())
                .build();

        when(receivableRepository.findByOrderId(100L)).thenReturn(List.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(List.of(tx));

        List<ReceivableResponse> responses = receivedService.findReceivableResponses(100L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        ReceivableResponse resp = responses.get(0);
        assertEquals(BigDecimal.valueOf(40.00), resp.getPaidAmount());
        assertEquals(BigDecimal.valueOf(60.00), resp.getRemainingAmount());
        assertEquals(BigDecimal.valueOf(60.00), resp.getPendingAmount());
        assertEquals("https://r2/file.pdf", resp.getPaymentFileUrl());
        assertEquals("https://pay.aura/1", resp.getPaymentUrl());
        assertEquals(1, resp.getTransactions().size());
        assertEquals(PaymentTransactionType.PAYMENT, resp.getTransactions().get(0).getTransactionType());
    }

    @Test
    @DisplayName("payReceivable — Deve registrar pagamento parcial e marcar status PARTIALLY_PAID")
    void shouldPayReceivablePartially() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(100.00))
                .status(ReceivableStatus.PENDING)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(40.00), PaymentMethod.PIX, null, null, null, "Primeira parte"
        );

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(Collections.emptyList());
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> {
            PaymentTransactionEntity t = inv.getArgument(0);
            t.setId(20L);
            return t;
        });
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceivableResponse response = receivedService.payReceivable(sampleOrder, 1L, request);

        assertNotNull(response);
        assertEquals(ReceivableStatus.PARTIALLY_PAID, response.getStatus());
        assertEquals(0, BigDecimal.valueOf(40.00).compareTo(response.getPaidAmount()));
        assertEquals(0, BigDecimal.valueOf(60.00).compareTo(response.getRemainingAmount()));
        assertEquals(0, BigDecimal.valueOf(60.00).compareTo(response.getPendingAmount()));
        assertEquals(1, response.getTransactions().size());
        assertEquals(PaymentTransactionType.PAYMENT, response.getTransactions().get(0).getTransactionType());
        verify(paymentTransactionRepository).save(any(PaymentTransactionEntity.class));
    }

    @Test
    @DisplayName("payReceivable — Deve registrar pagamento total e marcar status PAID")
    void shouldPayReceivableFully() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(60.00))
                .status(ReceivableStatus.PARTIALLY_PAID)
                .build();

        PaymentTransactionEntity prevTx = PaymentTransactionEntity.builder()
                .id(10L)
                .receivableId(1L)
                .transactionType(PaymentTransactionType.PAYMENT)
                .amountPaid(BigDecimal.valueOf(40.00))
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(60.00), PaymentMethod.CREDIT_CARD, null, null, null, "Quitação"
        );

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(List.of(prevTx));
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceivableResponse response = receivedService.payReceivable(sampleOrder, 1L, request);

        assertNotNull(response);
        assertEquals(ReceivableStatus.PAID, response.getStatus());
        assertEquals(BigDecimal.valueOf(100.00), response.getPaidAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRemainingAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getPendingAmount()));
        assertEquals(2, response.getTransactions().size());
    }

    @Test
    @DisplayName("payReceivable — Deve lançar UNPROCESSABLE_ENTITY se valor exceder saldo")
    void shouldThrow422WhenPaymentExceedsRemaining() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(100.00))
                .status(ReceivableStatus.PENDING)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(150.00), PaymentMethod.PIX, null, null, null, null
        );

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.payReceivable(sampleOrder, 1L, request)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("excede o saldo restante"));
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("payReceivable — Deve lançar UNPROCESSABLE_ENTITY se recebível estiver cancelado")
    void shouldThrow422WhenPayingCanceledReceivable() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.ZERO)
                .status(ReceivableStatus.CANCELED)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(50.00), PaymentMethod.PIX, null, null, null, null
        );

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.payReceivable(sampleOrder, 1L, request)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Não é possível registrar pagamento para uma parcela cancelada."));
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addReceivable — Deve adicionar novo recebível não pago e auto-incrementar installmentNumber")
    void shouldAddNewUnpaidReceivable() {
        ReceivableCreateRequest request = new ReceivableCreateRequest(
                null, LocalDate.now().plusDays(15), BigDecimal.valueOf(50.00),
                PaymentMethod.BOLETO, false, null, null, "123456", "http://boleto", "https://pay.aura/boleto", "Obs"
        );

        when(receivableRepository.findMaxInstallmentNumberByOrderId(100L)).thenReturn(1);
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> {
            ReceivableEntity e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        ReceivableResponse response = receivedService.createReceivable(sampleOrder, request);

        assertNotNull(response);
        assertEquals(2, response.getInstallmentNumber());
        assertEquals(ReceivableStatus.PENDING, response.getStatus());
        assertEquals("http://boleto", response.getPaymentFileUrl());
        assertEquals("https://pay.aura/boleto", response.getPaymentUrl());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addReceivable — Deve adicionar novo recebível já pago criando transação inicial")
    void shouldAddNewPaidReceivable() {
        ReceivableCreateRequest request = new ReceivableCreateRequest(
                1, LocalDate.now(), BigDecimal.valueOf(80.00),
                PaymentMethod.CREDIT_CARD, true, "MASTERCARD", "AUTH999", null, null, null, null
        );

        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> {
            ReceivableEntity e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> {
            PaymentTransactionEntity t = inv.getArgument(0);
            t.setId(30L);
            return t;
        });

        ReceivableResponse response = receivedService.createReceivable(sampleOrder, request);

        assertNotNull(response);
        assertEquals(ReceivableStatus.PAID, response.getStatus());
        assertEquals(BigDecimal.valueOf(80.00), response.getPaidAmount());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        assertEquals(1, response.getTransactions().size());
        assertEquals(PaymentTransactionType.PAYMENT, response.getTransactions().get(0).getTransactionType());
        verify(paymentTransactionRepository).save(any(PaymentTransactionEntity.class));
    }

    @Test
    @DisplayName("cancelReceivable — Deve cancelar recebível não pago com sucesso")
    void shouldCancelUnpaidReceivableSuccessfully() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .status(ReceivableStatus.PENDING)
                .amount(BigDecimal.valueOf(50.00))
                .pendingAmount(BigDecimal.valueOf(50.00))
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(Collections.emptyList());
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceivableResponse response = receivedService.cancelReceivable(sampleOrder, 1L);

        assertNotNull(response);
        assertEquals(ReceivableStatus.CANCELED, response.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        verify(receivableRepository).save(receivable);
    }

    @Test
    @DisplayName("cancelReceivable — Deve lançar UNPROCESSABLE_ENTITY se recebível já estiver pago")
    void shouldThrow422WhenCancelingPaidReceivable() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .status(ReceivableStatus.PAID)
                .pendingAmount(BigDecimal.ZERO)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.cancelReceivable(sampleOrder, 1L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(receivableRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadPaymentFile — Deve fazer upload do arquivo com sucesso e atualizar o recebível")
    void shouldUploadPaymentFileSuccessfully() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .status(ReceivableStatus.PENDING)
                .amount(BigDecimal.valueOf(50.00))
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "boleto.pdf", "application/pdf", "content".getBytes());

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));
        when(r2Service.upload(eq(file), eq("payment-files/10/100"))).thenReturn("key-123");
        when(r2Service.getFileUrl("key-123")).thenReturn("http://r2/boleto.pdf");
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(Collections.emptyList());

        ReceivableResponse response = receivedService.uploadPaymentFile(sampleOrder, 1L, file, "CODE-999");

        assertNotNull(response);
        assertEquals("http://r2/boleto.pdf", response.getPaymentFileUrl());
        assertEquals("CODE-999", response.getBankSlipCode());
        verify(r2Service).upload(file, "payment-files/10/100");
        verify(receivableRepository).save(receivable);
    }

    @Test
    @DisplayName("uploadPaymentFile — Deve lançar BAD_REQUEST quando arquivo for nulo ou vazio")
    void shouldThrowBadRequestWhenFileIsEmpty() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .partnerId(5L)
                .status(ReceivableStatus.PENDING)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "application/pdf", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.uploadPaymentFile(sampleOrder, 1L, emptyFile, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(r2Service, never()).upload(any(), any());
    }

    @Test
    @DisplayName("uploadPaymentFile — Deve lançar UNPROCESSABLE_ENTITY se recebível estiver cancelado")
    void shouldThrowUnprocessableEntityWhenUploadingToCanceledReceivable() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .status(ReceivableStatus.CANCELED)
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "boleto.pdf", "application/pdf", "content".getBytes());
        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.uploadPaymentFile(sampleOrder, 1L, file, null)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(r2Service, never()).upload(any(), any());
    }

    @Test
    @DisplayName("getPaymentFileUrl — Deve gerar url temporária para download com sucesso")
    void shouldGetPaymentFileUrlSuccessfully() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .paymentFileUrl("http://r2/boleto.pdf")
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));
        when(r2Service.generateTemporaryUrl(eq("http://r2/boleto.pdf"), eq(Duration.ofMinutes(5))))
                .thenReturn("http://r2/presigned-url");

        String url = receivedService.getPaymentFileUrl(sampleOrder, 1L);

        assertEquals("http://r2/presigned-url", url);
    }

    @Test
    @DisplayName("getPaymentFileUrl — Deve lançar NOT_FOUND quando recebível não possuir arquivo")
    void shouldThrowNotFoundWhenReceivableHasNoPaymentFileUrl() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .paymentFileUrl(null)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.getPaymentFileUrl(sampleOrder, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("deletePaymentFile — Deve excluir arquivo com sucesso e limpar os campos do recebível")
    void shouldDeletePaymentFileSuccessfully() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .paymentFileUrl("http://r2/boleto.pdf")
                .bankSlipCode("CODE-999")
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));
        when(r2Service.extractKeyFromUrl("http://r2/boleto.pdf")).thenReturn("key-123");

        receivedService.deletePaymentFile(sampleOrder, 1L);

        assertNull(receivable.getPaymentFileUrl());
        assertNull(receivable.getBankSlipCode());
        verify(r2Service).delete("key-123");
        verify(receivableRepository).save(receivable);
    }

    @Test
    @DisplayName("deletePaymentFile — Deve lançar NOT_FOUND quando recebível não possuir arquivo")
    void shouldThrowNotFoundWhenDeletingNonExistentPaymentFile() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .paymentFileUrl(null)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.deletePaymentFile(sampleOrder, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(r2Service, never()).delete(any());
    }

    @Test
    @DisplayName("deletePaymentFile — Deve lançar UNPROCESSABLE_ENTITY se recebível estiver cancelado")
    void shouldThrowUnprocessableEntityWhenDeletingFromCanceledReceivable() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(10L)
                .status(ReceivableStatus.CANCELED)
                .paymentFileUrl("http://r2/boleto.pdf")
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.deletePaymentFile(sampleOrder, 1L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(r2Service, never()).delete(any());
    }

    @Test
    @DisplayName("listReceivables — Deve listar recebíveis sem filtros opcionais com sucesso")
    void shouldListReceivablesSuccessfully() {
        Pageable pageable = PageRequest.of(0, 20);
        ReceivableSummaryResponse summary = new ReceivableSummaryResponse(
                1L, 100L, null, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00), ReceivableStatus.PENDING, PaymentMethod.BOLETO, LocalDate.of(2026, 12, 1)
        );
        Page<ReceivableSummaryResponse> expectedPage = new PageImpl<>(List.of(summary), pageable, 1);

        when(receivableRepository.searchSummarized(
                eq(10L), isNull(), eq(false), eq(false), eq(false), isNull(), isNull(), eq(false), anyList(), eq(pageable)
        )).thenReturn(expectedPage);

        Page<ReceivableSummaryResponse> result = receivedService.listReceivables(
                null, null, null, null, 10L, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(summary, result.getContent().get(0));
        verify(receivableRepository).searchSummarized(
                eq(10L), isNull(), eq(false), eq(false), eq(false), isNull(), isNull(), eq(false), anyList(), eq(pageable)
        );
    }

    @Test
    @DisplayName("listReceivables — Deve listar recebíveis com filtros opcionais de busca, data e status")
    void shouldListReceivablesWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<ReceivableStatus> statuses = List.of(ReceivableStatus.PENDING, ReceivableStatus.PARTIALLY_PAID);

        ReceivableSummaryResponse summary = new ReceivableSummaryResponse(
                1L, 100L, null, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00), ReceivableStatus.PARTIALLY_PAID, PaymentMethod.PIX, LocalDate.of(2026, 11, 15)
        );
        Page<ReceivableSummaryResponse> expectedPage = new PageImpl<>(List.of(summary), pageable, 1);

        when(receivableRepository.searchSummarized(
                eq(10L), eq("Cliente"), eq(true), eq(false), eq(false), eq(startDate), eq(endDate), eq(true), eq(statuses), eq(pageable)
        )).thenReturn(expectedPage);

        Page<ReceivableSummaryResponse> result = receivedService.listReceivables(
                "  Cliente  ", startDate, endDate, statuses, 10L, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(summary, result.getContent().get(0));
        verify(receivableRepository).searchSummarized(
                eq(10L), eq("Cliente"), eq(true), eq(false), eq(false), eq(startDate), eq(endDate), eq(true), eq(statuses), eq(pageable)
        );
    }

    @Test
    @DisplayName("listReceivables — Deve lançar BAD_REQUEST quando startDate for posterior a endDate")
    void shouldThrowBadRequestWhenStartDateAfterEndDateInListReceivables() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 12, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.listReceivables(null, startDate, endDate, null, 10L, pageable)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(receivableRepository, never()).searchSummarized(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any(), anyBoolean(), any(), any());
    }
}
