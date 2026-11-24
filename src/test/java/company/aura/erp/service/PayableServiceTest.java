package company.aura.erp.service;

import company.aura.erp.dto.payable.PayableRequest;
import company.aura.erp.dto.payable.PayableResponse;
import company.aura.erp.dto.payable.PayableSummaryResponse;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.entity.*;
import company.aura.erp.enums.*;
import company.aura.erp.modules.bucket.service.CloudflareR2Service;
import company.aura.erp.repository.*;
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
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayableServiceTest {

    @Mock
    private PayableRepository payableRepository;
    @Mock
    private PayableTransactionRepository payableTransactionRepository;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private CollaboratorRepository collaboratorRepository;
    @Mock
    private CommissionStatementRepository commissionStatementRepository;
    @Mock
    private CommissionEntryRepository commissionEntryRepository;
    @Mock
    private CloudflareR2Service r2Service;

    @InjectMocks
    private PayableService payableService;

    @Test
    @DisplayName("Deve criar conta a pagar pendente para parceiro")
    void shouldCreatePendingPayable() {
        PayableRequest request = PayableRequest.builder()
                .partnerId(5L)
                .description("Aluguel")
                .dueDate(LocalDate.of(2026, 12, 10))
                .amount(new BigDecimal("1000.00"))
                .paid(false)
                .build();

        when(partnerRepository.findByIdAndCompanyIdAndStatusNot(5L, 10L, PartnerStatus.DELETED.name()))
                .thenReturn(Optional.of(PartnerEntity.builder().id(5L).companyId(10L).name("Imobiliária").build()));
        when(payableRepository.save(any(PayableEntity.class))).thenAnswer(inv -> {
            PayableEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        PayableResponse response = payableService.createPayable(10L, request);

        assertEquals(1L, response.getId());
        assertEquals(PayableStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("1000.00"), response.getPendingAmount());
        assertTrue(response.getTransactions().isEmpty());
    }

    @Test
    @DisplayName("Deve criar conta a pagar já paga com transação inicial")
    void shouldCreatePaidPayableWithTransaction() {
        PayableRequest request = PayableRequest.builder()
                .partnerId(5L)
                .description("Internet")
                .dueDate(LocalDate.of(2026, 12, 10))
                .amount(new BigDecimal("150.00"))
                .paymentMethod(PaymentMethod.PIX)
                .paid(true)
                .notes("Pago no ato")
                .build();

        when(partnerRepository.findByIdAndCompanyIdAndStatusNot(5L, 10L, PartnerStatus.DELETED.name()))
                .thenReturn(Optional.of(PartnerEntity.builder().id(5L).companyId(10L).name("Provedor").build()));
        when(payableRepository.save(any(PayableEntity.class))).thenAnswer(inv -> {
            PayableEntity entity = inv.getArgument(0);
            entity.setId(2L);
            return entity;
        });
        when(payableTransactionRepository.save(any(PayableTransactionEntity.class))).thenAnswer(inv -> {
            PayableTransactionEntity tx = inv.getArgument(0);
            tx.setId(10L);
            return tx;
        });

        PayableResponse response = payableService.createPayable(10L, request);

        assertEquals(2L, response.getId());
        assertEquals(PayableStatus.PAID, response.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), response.getPendingAmount());
        assertEquals(new BigDecimal("150.00"), response.getPaidAmount());
        assertEquals(1, response.getTransactions().size());
        assertEquals(10L, response.getTransactions().get(0).getId());
    }

    @Test
    @DisplayName("Deve exigir partnerId ou collaboratorId ao criar conta a pagar")
    void shouldRejectPayableWithoutBeneficiary() {
        PayableRequest request = PayableRequest.builder()
                .description("Sem beneficiário")
                .dueDate(LocalDate.now())
                .amount(new BigDecimal("50.00"))
                .paid(false)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> payableService.createPayable(10L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve registrar pagamento total e sincronizar fechamento de comissão")
    void shouldPayCommissionPayableAndSyncStatement() {
        PayableEntity payable = PayableEntity.builder()
                .id(1L)
                .companyId(10L)
                .collaboratorId(5L)
                .originType(PayableOriginType.COMMISSION_STATEMENT)
                .originId(20L)
                .description("Comissão João")
                .amount(new BigDecimal("200.00"))
                .pendingAmount(new BigDecimal("200.00"))
                .status(PayableStatus.PENDING)
                .build();

        CommissionStatementEntity statement = CommissionStatementEntity.builder()
                .id(20L)
                .companyId(10L)
                .payableId(1L)
                .status(CommissionStatementStatus.APPROVED)
                .build();
        CommissionEntryEntity entry = CommissionEntryEntity.builder()
                .id(30L)
                .companyId(10L)
                .statementId(20L)
                .status(CommissionEntryStatus.APPROVED)
                .build();

        PayableTransactionRequest txRequest = new PayableTransactionRequest(
                new BigDecimal("200.00"), PaymentMethod.PIX, null, null, "Pgto total"
        );

        when(payableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(payable));
        when(payableTransactionRepository.findByPayableId(1L)).thenReturn(Collections.emptyList());
        when(payableRepository.save(any(PayableEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payableTransactionRepository.save(any(PayableTransactionEntity.class))).thenAnswer(inv -> {
            PayableTransactionEntity tx = inv.getArgument(0);
            tx.setId(50L);
            return tx;
        });
        when(commissionStatementRepository.findByPayableIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(statement));
        when(commissionEntryRepository.findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(20L, 10L))
                .thenReturn(List.of(entry));

        PayableResponse response = payableService.payPayable(10L, 1L, txRequest);

        assertEquals(PayableStatus.PAID, response.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), response.getPendingAmount());
        assertEquals(CommissionStatementStatus.PAID, statement.getStatus());
        assertEquals(CommissionEntryStatus.PAID, entry.getStatus());
    }

    @Test
    @DisplayName("Deve permitir estorno (REVERSAL) parcial de pagamento")
    void shouldReversePayment() {
        PayableEntity payable = PayableEntity.builder()
                .id(1L)
                .companyId(10L)
                .collaboratorId(5L)
                .originType(PayableOriginType.MANUAL)
                .description("Despesa")
                .amount(new BigDecimal("200.00"))
                .pendingAmount(BigDecimal.ZERO)
                .status(PayableStatus.PAID)
                .build();

        PayableTransactionEntity existingPayment = PayableTransactionEntity.builder()
                .id(50L)
                .payableId(1L)
                .companyId(10L)
                .transactionType(PayableTransactionType.PAYMENT)
                .amountPaid(new BigDecimal("200.00"))
                .paymentMethod(PaymentMethod.PIX)
                .build();

        PayableTransactionRequest reversalRequest = new PayableTransactionRequest(
                new BigDecimal("50.00"), PaymentMethod.PIX, null, null, "Estorno parcial", PayableTransactionType.REVERSAL
        );

        when(payableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(payable));
        when(payableTransactionRepository.findByPayableId(1L)).thenReturn(List.of(existingPayment));
        when(payableRepository.save(any(PayableEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payableTransactionRepository.save(any(PayableTransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PayableResponse response = payableService.payPayable(10L, 1L, reversalRequest);

        assertEquals(PayableStatus.PARTIALLY_PAID, response.getStatus());
        assertEquals(new BigDecimal("50.00"), response.getPendingAmount());
        assertEquals(new BigDecimal("150.00"), response.getPaidAmount());
    }

    @Test
    @DisplayName("Deve cancelar conta a pagar sem transações")
    void shouldCancelPayable() {
        PayableEntity payable = PayableEntity.builder()
                .id(1L)
                .companyId(10L)
                .status(PayableStatus.PENDING)
                .amount(new BigDecimal("100.00"))
                .pendingAmount(new BigDecimal("100.00"))
                .build();

        when(payableRepository.findByIdAndCompanyId(1L, 10L)).thenReturn(Optional.of(payable));
        when(payableTransactionRepository.findByPayableId(1L)).thenReturn(Collections.emptyList());
        when(payableRepository.save(payable)).thenReturn(payable);

        PayableResponse response = payableService.cancelPayable(10L, 1L);

        assertEquals(PayableStatus.CANCELED, response.getStatus());
    }

    @Test
    @DisplayName("Deve fazer upload, consultar URL temporária e remover comprovante")
    void shouldManageReceiptFile() {
        PayableEntity payable = PayableEntity.builder()
                .id(2L)
                .companyId(10L)
                .status(PayableStatus.PAID)
                .amount(new BigDecimal("100.00"))
                .pendingAmount(BigDecimal.ZERO)
                .build();
        PayableTransactionEntity tx = PayableTransactionEntity.builder()
                .id(99L)
                .payableId(2L)
                .companyId(10L)
                .transactionType(PayableTransactionType.PAYMENT)
                .amountPaid(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.PIX)
                .build();

        MockMultipartFile file = new MockMultipartFile("file", "recibo.pdf", "application/pdf", "pdf".getBytes());

        when(payableRepository.findByIdAndCompanyId(2L, 10L)).thenReturn(Optional.of(payable));
        when(payableTransactionRepository.findTopByPayableIdAndCompanyIdOrderByPaidAtDescIdDesc(2L, 10L))
                .thenReturn(Optional.of(tx));
        when(r2Service.upload(file, "payable-files/10/2")).thenReturn("payable-files/10/2/recibo.pdf");
        when(r2Service.getFileUrl("payable-files/10/2/recibo.pdf")).thenReturn("https://cdn/recibo.pdf");
        when(payableTransactionRepository.findByPayableId(2L)).thenReturn(List.of(tx));
        when(r2Service.generateTemporaryUrl("https://cdn/recibo.pdf", Duration.ofMinutes(5))).thenReturn("https://tmp/recibo.pdf");
        when(r2Service.extractKeyFromUrl("https://cdn/recibo.pdf")).thenReturn("payable-files/10/2/recibo.pdf");

        payableService.uploadPaymentFile(10L, 2L, file, null);
        assertEquals("https://cdn/recibo.pdf", tx.getReceiptUrl());

        String tempUrl = payableService.getPaymentFileUrl(10L, 2L, null);
        assertEquals("https://tmp/recibo.pdf", tempUrl);

        payableService.deletePaymentFile(10L, 2L, null);
        assertNull(tx.getReceiptUrl());
        verify(r2Service).delete("payable-files/10/2/recibo.pdf");
    }

    @Test
    @DisplayName("Deve listar contas a pagar com filtro padrão PENDING quando status não informado")
    void shouldListPayables() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<PayableSummaryResponse> expected = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(payableRepository.searchSummarized(eq(10L), isNull(), eq(false), eq(false), eq(false),
                isNull(), isNull(), eq(false), eq(List.of(PayableStatus.PENDING)), eq(false), isNull(), eq(pageable)))
                .thenReturn(expected);

        Page<PayableSummaryResponse> result = payableService.listPayables(null, null, null, null, null, 10L, pageable);

        assertEquals(0, result.getTotalElements());
    }
}
