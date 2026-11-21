package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.entity.ReceivableEntity;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.PaymentTransactionType;
import company.aura.erp.enums.ReceivableOriginType;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.modules.bucket.service.CloudflareR2Service;
import company.aura.erp.repository.PaymentTransactionRepository;
import company.aura.erp.repository.ReceivableRepository;
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

    private static final Long COMPANY_ID = 10L;

    @Test
    @DisplayName("Deve processar recebíveis com sucesso criando parcelas e transações para as pagas")
    void shouldProcessReceivablesSuccessfully() {
        List<ReceivableRequest> receivables = List.of(
                new ReceivableRequest(
                        100L, null, ReceivableOriginType.ORDER, 5L, 1, LocalDate.now(), BigDecimal.valueOf(100.00),
                        PaymentMethod.PIX, true, null, null, null, null, "https://pay.aura/pix-1", null
                ),
                new ReceivableRequest(
                        100L, null, ReceivableOriginType.ORDER, 5L, 2, LocalDate.now().plusDays(30), BigDecimal.valueOf(35.00),
                        PaymentMethod.CREDIT_CARD, false, "VISA", "123456", null, null, "https://pay.aura/card-2", null
                )
        );

        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> {
            ReceivableEntity entity = inv.getArgument(0);
            entity.setId(500L);
            return entity;
        });
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> {
            PaymentTransactionEntity tx = inv.getArgument(0);
            tx.setId(10L);
            return tx;
        });

        receivedService.processReceivables(receivables, COMPANY_ID);

        ArgumentCaptor<ReceivableEntity> recCaptor = ArgumentCaptor.forClass(ReceivableEntity.class);
        verify(receivableRepository, times(2)).save(recCaptor.capture());
        List<ReceivableEntity> savedReceivables = recCaptor.getAllValues();

        assertEquals(ReceivableOriginType.ORDER, savedReceivables.get(0).getOriginType());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), savedReceivables.get(0).getPendingAmount());
        assertEquals(ReceivableStatus.PAID, savedReceivables.get(0).getStatus());
        assertEquals("https://pay.aura/pix-1", savedReceivables.get(0).getPaymentUrl());

        assertEquals(ReceivableOriginType.ORDER, savedReceivables.get(1).getOriginType());
        assertEquals(BigDecimal.valueOf(35.00).setScale(2, RoundingMode.HALF_UP), savedReceivables.get(1).getPendingAmount());
        assertEquals(ReceivableStatus.PENDING, savedReceivables.get(1).getStatus());
        assertEquals("https://pay.aura/card-2", savedReceivables.get(1).getPaymentUrl());

        ArgumentCaptor<PaymentTransactionEntity> txCaptor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionRepository, times(1)).save(txCaptor.capture());
        assertEquals(PaymentTransactionType.PAYMENT, txCaptor.getValue().getTransactionType());
        assertEquals(500L, txCaptor.getValue().getReceivableId());
    }

    @Test
    @DisplayName("findReceivableResponses — Deve retornar recebíveis com suas transações e saldo calculado")
    void shouldFindReceivableResponsesWithTransactions() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .originType(ReceivableOriginType.ORDER)
                .companyId(COMPANY_ID)
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
                .companyId(COMPANY_ID)
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
        assertEquals(ReceivableOriginType.ORDER, resp.getOriginType());
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
                .originType(ReceivableOriginType.ORDER)
                .companyId(COMPANY_ID)
                .partnerId(5L)
                .installmentNumber(1)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(100.00))
                .status(ReceivableStatus.PENDING)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(40.00), PaymentMethod.PIX, null, null, null, "Primeira parte"
        );

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(Collections.emptyList());
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> {
            PaymentTransactionEntity t = inv.getArgument(0);
            t.setId(20L);
            return t;
        });
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceivableResponse response = receivedService.payReceivable(COMPANY_ID, 1L, request);

        assertNotNull(response);
        assertEquals(ReceivableOriginType.ORDER, response.getOriginType());
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
                .originType(ReceivableOriginType.ORDER)
                .companyId(COMPANY_ID)
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

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(List.of(prevTx));
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceivableResponse response = receivedService.payReceivable(COMPANY_ID, 1L, request);

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
                .companyId(COMPANY_ID)
                .partnerId(5L)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.valueOf(100.00))
                .status(ReceivableStatus.PENDING)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(150.00), PaymentMethod.PIX, null, null, null, null
        );

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.payReceivable(COMPANY_ID, 1L, request)
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
                .companyId(COMPANY_ID)
                .partnerId(5L)
                .amount(BigDecimal.valueOf(100.00))
                .pendingAmount(BigDecimal.ZERO)
                .status(ReceivableStatus.CANCELED)
                .build();

        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(50.00), PaymentMethod.PIX, null, null, null, null
        );

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.payReceivable(COMPANY_ID, 1L, request)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Não é possível registrar pagamento para uma parcela cancelada."));
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("processSingleReceivable — Deve adicionar novo recebível não pago")
    void shouldAddNewUnpaidReceivable() {
        ReceivableRequest request = new ReceivableRequest(
                100L, null, ReceivableOriginType.ORDER, 5L, 2, LocalDate.now().plusDays(15), BigDecimal.valueOf(50.00),
                PaymentMethod.BOLETO, false, null, null, "123456", "http://boleto", "https://pay.aura/boleto", "Obs"
        );

        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> {
            ReceivableEntity e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        ReceivableResponse response = receivedService.processSingleReceivable(COMPANY_ID, request);

        assertNotNull(response);
        assertEquals(ReceivableOriginType.ORDER, response.getOriginType());
        assertEquals(2, response.getInstallmentNumber());
        assertEquals(ReceivableStatus.PENDING, response.getStatus());
        assertEquals("http://boleto", response.getPaymentFileUrl());
        assertEquals("https://pay.aura/boleto", response.getPaymentUrl());
        assertEquals(BigDecimal.valueOf(50.00).setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("processSingleReceivable — Deve adicionar novo recebível já pago criando transação inicial")
    void shouldAddNewPaidReceivable() {
        ReceivableRequest request = new ReceivableRequest(
                null, 200L, ReceivableOriginType.SERVICE_ORDER, 5L, 1, LocalDate.now(), BigDecimal.valueOf(80.00),
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

        ReceivableResponse response = receivedService.processSingleReceivable(COMPANY_ID, request);

        assertNotNull(response);
        assertEquals(200L, response.getServiceOrderId());
        assertEquals(ReceivableOriginType.SERVICE_ORDER, response.getOriginType());
        assertEquals(ReceivableStatus.PAID, response.getStatus());
        assertEquals(BigDecimal.valueOf(80.00), response.getPaidAmount());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), response.getPendingAmount());
        assertEquals(1, response.getTransactions().size());
        assertEquals(PaymentTransactionType.PAYMENT, response.getTransactions().get(0).getTransactionType());
        verify(paymentTransactionRepository).save(any(PaymentTransactionEntity.class));
    }

    @Test
    @DisplayName("processSingleReceivable — Deve inferir originType MANUAL quando originType, orderId e serviceOrderId forem nulos")
    void shouldInferManualOriginTypeWhenOriginTypeAndIdsAreNull() {
        ReceivableRequest request = new ReceivableRequest(
                null, null, null, 5L, 1, LocalDate.now(), BigDecimal.valueOf(60.00),
                PaymentMethod.PIX, false, null, null, null, null, null, "Avulso"
        );

        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> {
            ReceivableEntity e = inv.getArgument(0);
            e.setId(4L);
            return e;
        });

        ReceivableResponse response = receivedService.processSingleReceivable(COMPANY_ID, request);

        assertNotNull(response);
        assertEquals(ReceivableOriginType.MANUAL, response.getOriginType());
    }

    @Test
    @DisplayName("cancelReceivable — Deve cancelar recebível não pago com sucesso")
    void shouldCancelUnpaidReceivableSuccessfully() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(COMPANY_ID)
                .status(ReceivableStatus.PENDING)
                .amount(BigDecimal.valueOf(50.00))
                .pendingAmount(BigDecimal.valueOf(50.00))
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(Collections.emptyList());
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceivableResponse response = receivedService.cancelReceivable(COMPANY_ID, 1L);

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
                .companyId(COMPANY_ID)
                .status(ReceivableStatus.PAID)
                .pendingAmount(BigDecimal.ZERO)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.cancelReceivable(COMPANY_ID, 1L)
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
                .companyId(COMPANY_ID)
                .partnerId(5L)
                .status(ReceivableStatus.PENDING)
                .amount(BigDecimal.valueOf(50.00))
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "boleto.pdf", "application/pdf", "content".getBytes());

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));
        when(r2Service.upload(eq(file), eq("payment-files/10/1"))).thenReturn("key-123");
        when(r2Service.getFileUrl("key-123")).thenReturn("http://r2/boleto.pdf");
        when(receivableRepository.save(any(ReceivableEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(Collections.emptyList());

        ReceivableResponse response = receivedService.uploadPaymentFile(COMPANY_ID, 1L, file, "CODE-999");

        assertNotNull(response);
        assertEquals("http://r2/boleto.pdf", response.getPaymentFileUrl());
        assertEquals("CODE-999", response.getBankSlipCode());
        verify(r2Service).upload(file, "payment-files/10/1");
        verify(receivableRepository).save(receivable);
    }

    @Test
    @DisplayName("uploadPaymentFile — Deve lançar BAD_REQUEST quando arquivo for nulo ou vazio")
    void shouldThrowBadRequestWhenFileIsEmpty() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(COMPANY_ID)
                .partnerId(5L)
                .status(ReceivableStatus.PENDING)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "application/pdf", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.uploadPaymentFile(COMPANY_ID, 1L, emptyFile, null)
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
                .companyId(COMPANY_ID)
                .status(ReceivableStatus.CANCELED)
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "boleto.pdf", "application/pdf", "content".getBytes());
        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.uploadPaymentFile(COMPANY_ID, 1L, file, null)
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
                .companyId(COMPANY_ID)
                .paymentFileUrl("http://r2/boleto.pdf")
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));
        when(r2Service.generateTemporaryUrl(eq("http://r2/boleto.pdf"), eq(Duration.ofMinutes(5))))
                .thenReturn("http://r2/presigned-url");

        String url = receivedService.getPaymentFileUrl(COMPANY_ID, 1L);

        assertEquals("http://r2/presigned-url", url);
    }

    @Test
    @DisplayName("getPaymentFileUrl — Deve lançar NOT_FOUND quando recebível não possuir arquivo")
    void shouldThrowNotFoundWhenReceivableHasNoPaymentFileUrl() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(COMPANY_ID)
                .paymentFileUrl(null)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.getPaymentFileUrl(COMPANY_ID, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("deletePaymentFile — Deve excluir arquivo com sucesso e limpar os campos do recebível")
    void shouldDeletePaymentFileSuccessfully() {
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .companyId(COMPANY_ID)
                .paymentFileUrl("http://r2/boleto.pdf")
                .bankSlipCode("CODE-999")
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));
        when(r2Service.extractKeyFromUrl("http://r2/boleto.pdf")).thenReturn("key-123");

        receivedService.deletePaymentFile(COMPANY_ID, 1L);

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
                .companyId(COMPANY_ID)
                .paymentFileUrl(null)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.deletePaymentFile(COMPANY_ID, 1L)
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
                .companyId(COMPANY_ID)
                .status(ReceivableStatus.CANCELED)
                .paymentFileUrl("http://r2/boleto.pdf")
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.deletePaymentFile(COMPANY_ID, 1L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(r2Service, never()).delete(any());
    }

    @Test
    @DisplayName("listReceivables — Deve listar recebíveis sem filtros opcionais com sucesso")
    void shouldListReceivablesSuccessfully() {
        Pageable pageable = PageRequest.of(0, 20);
        ReceivableSummaryResponse summary = new ReceivableSummaryResponse(
                1L, 100L, null, ReceivableOriginType.ORDER, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00), ReceivableStatus.PENDING, PaymentMethod.BOLETO, LocalDate.of(2026, 12, 1)
        );
        Page<ReceivableSummaryResponse> expectedPage = new PageImpl<>(List.of(summary), pageable, 1);

        when(receivableRepository.searchSummarized(
                eq(COMPANY_ID), isNull(), eq(false), eq(false), eq(false), isNull(), isNull(), eq(false), anyList(), eq(pageable)
        )).thenReturn(expectedPage);

        Page<ReceivableSummaryResponse> result = receivedService.listReceivables(
                null, null, null, null, COMPANY_ID, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(summary, result.getContent().get(0));
        verify(receivableRepository).searchSummarized(
                eq(COMPANY_ID), isNull(), eq(false), eq(false), eq(false), isNull(), isNull(), eq(false), anyList(), eq(pageable)
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
                1L, 100L, null, ReceivableOriginType.ORDER, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00), ReceivableStatus.PARTIALLY_PAID, PaymentMethod.PIX, LocalDate.of(2026, 11, 15)
        );
        Page<ReceivableSummaryResponse> expectedPage = new PageImpl<>(List.of(summary), pageable, 1);

        when(receivableRepository.searchSummarized(
                eq(COMPANY_ID), eq("Cliente"), eq(true), eq(false), eq(false), eq(startDate), eq(endDate), eq(true), eq(statuses), eq(pageable)
        )).thenReturn(expectedPage);

        Page<ReceivableSummaryResponse> result = receivedService.listReceivables(
                "  Cliente  ", startDate, endDate, statuses, COMPANY_ID, pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(summary, result.getContent().get(0));
        verify(receivableRepository).searchSummarized(
                eq(COMPANY_ID), eq("Cliente"), eq(true), eq(false), eq(false), eq(startDate), eq(endDate), eq(true), eq(statuses), eq(pageable)
        );
    }

    @Test
    @DisplayName("listReceivables — Deve lançar BAD_REQUEST quando startDate for posterior a endDate")
    void shouldThrowBadRequestWhenStartDateAfterEndDateInListReceivables() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 12, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.listReceivables(null, startDate, endDate, null, COMPANY_ID, pageable)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(receivableRepository, never()).searchSummarized(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("findById — Deve retornar todas as informações do recebível e suas transações")
    void shouldFindReceivableByIdWithAllInformation() {
        LocalDate dueDate = LocalDate.of(2026, 12, 10);
        LocalDateTime now = LocalDateTime.now();
        ReceivableEntity receivable = ReceivableEntity.builder()
                .id(1L)
                .orderId(100L)
                .serviceOrderId(200L)
                .originType(ReceivableOriginType.SERVICE_ORDER)
                .companyId(COMPANY_ID)
                .partnerId(5L)
                .installmentNumber(2)
                .dueDate(dueDate)
                .amount(BigDecimal.valueOf(150.00))
                .pendingAmount(BigDecimal.valueOf(50.00))
                .paymentMethod(PaymentMethod.BOLETO)
                .status(ReceivableStatus.PARTIALLY_PAID)
                .bankSlipCode("34191.79001")
                .paymentFileUrl("https://r2/boleto.pdf")
                .paymentUrl("https://pay.aura/1")
                .notes("Segunda parcela")
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                .id(10L)
                .receivableId(1L)
                .companyId(COMPANY_ID)
                .transactionType(PaymentTransactionType.PAYMENT)
                .amountPaid(BigDecimal.valueOf(100.00))
                .paymentMethod(PaymentMethod.PIX)
                .paidAt(now)
                .build();

        when(receivableRepository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(receivable));
        when(paymentTransactionRepository.findByReceivableId(1L)).thenReturn(List.of(tx));

        ReceivableResponse response = receivedService.findById(COMPANY_ID, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getOrderId());
        assertEquals(200L, response.getServiceOrderId());
        assertEquals(ReceivableOriginType.SERVICE_ORDER, response.getOriginType());
        assertEquals(COMPANY_ID, response.getCompanyId());
        assertEquals(5L, response.getPartnerId());
        assertEquals(2, response.getInstallmentNumber());
        assertEquals(dueDate, response.getDueDate());
        assertEquals(BigDecimal.valueOf(150.00), response.getAmount());
        assertEquals(BigDecimal.valueOf(100.00), response.getPaidAmount());
        assertEquals(BigDecimal.valueOf(50.00), response.getRemainingAmount());
        assertEquals(BigDecimal.valueOf(50.00), response.getPendingAmount());
        assertEquals(PaymentMethod.BOLETO, response.getPaymentMethod());
        assertEquals(ReceivableStatus.PARTIALLY_PAID, response.getStatus());
        assertEquals("34191.79001", response.getBankSlipCode());
        assertEquals("https://r2/boleto.pdf", response.getPaymentFileUrl());
        assertEquals("https://pay.aura/1", response.getPaymentUrl());
        assertEquals("Segunda parcela", response.getNotes());
        assertEquals(1, response.getTransactions().size());
        verify(receivableRepository).findByIdAndCompanyId(1L, COMPANY_ID);
        verify(paymentTransactionRepository).findByReceivableId(1L);
    }

    @Test
    @DisplayName("findById — Deve lançar NOT_FOUND quando recebível não for encontrado")
    void shouldThrowNotFoundWhenReceivableDoesNotExistOnFindById() {
        when(receivableRepository.findByIdAndCompanyId(999L, COMPANY_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                receivedService.findById(COMPANY_ID, 999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(paymentTransactionRepository, never()).findByReceivableId(any());
    }
}
