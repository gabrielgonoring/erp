package company.aura.erp.controller;

import company.aura.erp.dto.order.*;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableOriginType;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.ReceivedService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceivableControllerTest {

    @Mock
    private ReceivedService receivedService;

    @InjectMocks
    private ReceivableController receivableController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("admin@empresa.com")
                .name("Admin")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("POST /api/receivables — Deve criar recebível e retornar HTTP 201")
    void shouldCreateReceivable() {
        ReceivableRequest request = new ReceivableRequest(
                1L, null, ReceivableOriginType.ORDER, 5L, 1, LocalDate.now(), BigDecimal.valueOf(50.00), PaymentMethod.PIX,
                false, null, null, null, null, null, null
        );
        ReceivableResponse responseDto = ReceivableResponse.builder()
                .id(1L)
                .orderId(1L)
                .originType(ReceivableOriginType.ORDER)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .dueDate(LocalDate.now())
                .amount(BigDecimal.valueOf(50.00))
                .paymentMethod(PaymentMethod.PIX)
                .status(ReceivableStatus.PENDING)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(BigDecimal.valueOf(50.00))
                .pendingAmount(BigDecimal.valueOf(50.00))
                .transactions(Collections.emptyList())
                .build();

        when(receivedService.processSingleReceivable(10L, request)).thenReturn(responseDto);

        ResponseEntity<ReceivableResponse> response = receivableController.createReceivable(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(receivedService).processSingleReceivable(10L, request);
    }

    @Test
    @DisplayName("POST /api/receivables/{receivableId}/pay — Deve registrar pagamento do recebível")
    void shouldPayReceivable() {
        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(25.00), PaymentMethod.PIX, null, null, null, "Parcial"
        );
        ReceivableResponse responseDto = ReceivableResponse.builder()
                .id(1L)
                .orderId(1L)
                .originType(ReceivableOriginType.ORDER)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .dueDate(LocalDate.now())
                .amount(BigDecimal.valueOf(50.00))
                .paymentMethod(PaymentMethod.PIX)
                .status(ReceivableStatus.PARTIALLY_PAID)
                .paidAmount(BigDecimal.valueOf(25.00))
                .remainingAmount(BigDecimal.valueOf(25.00))
                .pendingAmount(BigDecimal.valueOf(25.00))
                .transactions(Collections.emptyList())
                .build();

        when(receivedService.payReceivable(10L, 1L, request)).thenReturn(responseDto);

        ResponseEntity<ReceivableResponse> response = receivableController.payReceivable(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(receivedService).payReceivable(10L, 1L, request);
    }

    @Test
    @DisplayName("DELETE /api/receivables/{receivableId} — Deve cancelar recebível e retornar HTTP 204")
    void shouldCancelReceivable() {
        ResponseEntity<Void> response = receivableController.cancelReceivable(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(receivedService).cancelReceivable(10L, 1L);
    }

    @Test
    @DisplayName("POST /api/receivables/{receivableId}/payment-file — Deve fazer upload do comprovante/boleto e retornar HTTP 200")
    void shouldUploadPaymentFile() {
        MockMultipartFile file = new MockMultipartFile("file", "boleto.pdf", "application/pdf", "content".getBytes());
        ReceivableResponse receivableResponse = new ReceivableResponse();
        receivableResponse.setId(2L);
        receivableResponse.setPaymentFileUrl("http://storage/boleto.pdf");

        when(receivedService.uploadPaymentFile(10L, 2L, file, "12345")).thenReturn(receivableResponse);

        ResponseEntity<ReceivableResponse> response = receivableController.uploadPaymentFile(2L, file, "12345", userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(receivableResponse, response.getBody());
        verify(receivedService).uploadPaymentFile(10L, 2L, file, "12345");
    }

    @Test
    @DisplayName("GET /api/receivables/{receivableId}/payment-file — Deve retornar URL temporária do arquivo de pagamento")
    void shouldGetPaymentFile() {
        when(receivedService.getPaymentFileUrl(10L, 2L)).thenReturn("http://storage/presigned-url");

        ResponseEntity<PaymentFileUrlResponse> response = receivableController.getPaymentFile(2L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new PaymentFileUrlResponse("http://storage/presigned-url"), response.getBody());
        verify(receivedService).getPaymentFileUrl(10L, 2L);
    }

    @Test
    @DisplayName("DELETE /api/receivables/{receivableId}/payment-file — Deve apagar o arquivo de pagamento do recebível e retornar HTTP 204")
    void shouldDeletePaymentFile() {
        ResponseEntity<Void> response = receivableController.deletePaymentFile(2L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(receivedService).deletePaymentFile(10L, 2L);
    }

    @Test
    @DisplayName("GET /api/receivables — Deve listar recebíveis de forma paginada sem filtros opcionais")
    void shouldListReceivablesWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        ReceivableSummaryResponse summary = new ReceivableSummaryResponse(
                1L, 100L, null, ReceivableOriginType.ORDER, 5L, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00), ReceivableStatus.PENDING, PaymentMethod.BOLETO, LocalDate.of(2026, 12, 1)
        );
        Page<ReceivableSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(receivedService.listReceivables(null, null, null, null, null, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<ReceivableSummaryResponse>> response = receivableController.listReceivables(null, null, null, null, null, pageable, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(receivedService).listReceivables(null, null, null, null, null, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/receivables — Deve listar recebíveis de forma paginada com filtros opcionais")
    void shouldListReceivablesWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<ReceivableStatus> statuses = List.of(ReceivableStatus.PENDING);
        List<PaymentMethod> paymentMethods = List.of(PaymentMethod.PIX);

        ReceivableSummaryResponse summary = new ReceivableSummaryResponse(
                1L, 100L, null, ReceivableOriginType.ORDER, 5L, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00), ReceivableStatus.PARTIALLY_PAID, PaymentMethod.PIX, LocalDate.of(2026, 11, 15)
        );
        Page<ReceivableSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(receivedService.listReceivables("Cliente", startDate, endDate, statuses, paymentMethods, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<ReceivableSummaryResponse>> response = receivableController.listReceivables("Cliente", startDate, endDate, statuses, paymentMethods, pageable, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(receivedService).listReceivables("Cliente", startDate, endDate, statuses, paymentMethods, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/receivables/{receivableId} — Deve buscar recebível por ID com todas as informações")
    void shouldFindReceivableById() {
        ReceivableResponse responseDto = ReceivableResponse.builder()
                .id(1L)
                .orderId(100L)
                .originType(ReceivableOriginType.ORDER)
                .companyId(10L)
                .partnerId(5L)
                .installmentNumber(1)
                .dueDate(LocalDate.of(2026, 12, 1))
                .amount(BigDecimal.valueOf(100.00))
                .paymentMethod(PaymentMethod.PIX)
                .status(ReceivableStatus.PAID)
                .paidAmount(BigDecimal.valueOf(100.00))
                .remainingAmount(BigDecimal.ZERO)
                .pendingAmount(BigDecimal.ZERO)
                .transactions(Collections.emptyList())
                .build();

        when(receivedService.findById(10L, 1L)).thenReturn(responseDto);

        ResponseEntity<ReceivableResponse> response = receivableController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(receivedService).findById(10L, 1L);
    }
}
