package company.aura.erp.controller;

import company.aura.erp.dto.order.PaymentFileUrlResponse;
import company.aura.erp.dto.payable.PayableRequest;
import company.aura.erp.dto.payable.PayableResponse;
import company.aura.erp.dto.payable.PayableSummaryResponse;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.enums.PayableOriginType;
import company.aura.erp.enums.PayableStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.PayableService;
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
class PayableControllerTest {

    @Mock
    private PayableService payableService;

    @InjectMocks
    private PayableController payableController;

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
    @DisplayName("POST /api/payables — Deve criar conta a pagar e retornar HTTP 201")
    void shouldCreatePayable() {
        PayableRequest request = PayableRequest.builder()
                .partnerId(5L)
                .originType(PayableOriginType.MANUAL)
                .description("Fornecedor XYZ")
                .installmentNumber(1)
                .dueDate(LocalDate.now())
                .amount(BigDecimal.valueOf(150.00))
                .paymentMethod(PaymentMethod.PIX)
                .paid(false)
                .build();

        PayableResponse expected = PayableResponse.builder()
                .id(1L)
                .companyId(10L)
                .partnerId(5L)
                .originType(PayableOriginType.MANUAL)
                .description("Fornecedor XYZ")
                .amount(BigDecimal.valueOf(150.00))
                .pendingAmount(BigDecimal.valueOf(150.00))
                .status(PayableStatus.PENDING)
                .transactions(Collections.emptyList())
                .build();

        when(payableService.createPayable(10L, request)).thenReturn(expected);

        ResponseEntity<PayableResponse> response = payableController.createPayable(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(payableService).createPayable(10L, request);
    }

    @Test
    @DisplayName("POST /api/payables/{payableId}/pay — Deve registrar pagamento da conta a pagar")
    void shouldPayPayable() {
        PayableTransactionRequest request = new PayableTransactionRequest(
                BigDecimal.valueOf(50.00), PaymentMethod.PIX, null, null, "Parcial"
        );
        PayableResponse expected = PayableResponse.builder()
                .id(1L)
                .companyId(10L)
                .status(PayableStatus.PARTIALLY_PAID)
                .paidAmount(BigDecimal.valueOf(50.00))
                .pendingAmount(BigDecimal.valueOf(100.00))
                .build();

        when(payableService.payPayable(10L, 1L, request)).thenReturn(expected);

        ResponseEntity<PayableResponse> response = payableController.payPayable(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(payableService).payPayable(10L, 1L, request);
    }

    @Test
    @DisplayName("DELETE /api/payables/{payableId} — Deve cancelar conta a pagar e retornar HTTP 204")
    void shouldCancelPayable() {
        ResponseEntity<Void> response = payableController.cancelPayable(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(payableService).cancelPayable(10L, 1L);
    }

    @Test
    @DisplayName("POST /api/payables/{payableId}/payment-file — Deve fazer upload do comprovante e retornar HTTP 200")
    void shouldUploadPaymentFile() {
        MockMultipartFile file = new MockMultipartFile("file", "comprovante.pdf", "application/pdf", "content".getBytes());
        PayableResponse expected = PayableResponse.builder().id(2L).build();

        when(payableService.uploadPaymentFile(10L, 2L, file, 99L)).thenReturn(expected);

        ResponseEntity<PayableResponse> response = payableController.uploadPaymentFile(2L, file, 99L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(payableService).uploadPaymentFile(10L, 2L, file, 99L);
    }

    @Test
    @DisplayName("GET /api/payables/{payableId}/payment-file — Deve retornar URL temporária do comprovante")
    void shouldGetPaymentFile() {
        when(payableService.getPaymentFileUrl(10L, 2L, null)).thenReturn("http://storage/presigned-url");

        ResponseEntity<PaymentFileUrlResponse> response = payableController.getPaymentFile(2L, null, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new PaymentFileUrlResponse("http://storage/presigned-url"), response.getBody());
        verify(payableService).getPaymentFileUrl(10L, 2L, null);
    }

    @Test
    @DisplayName("DELETE /api/payables/{payableId}/payment-file — Deve apagar o comprovante e retornar HTTP 204")
    void shouldDeletePaymentFile() {
        ResponseEntity<Void> response = payableController.deletePaymentFile(2L, 99L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(payableService).deletePaymentFile(10L, 2L, 99L);
    }

    @Test
    @DisplayName("GET /api/payables — Deve listar contas a pagar paginadas")
    void shouldListPayables() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<PayableStatus> statuses = List.of(PayableStatus.PENDING);
        List<PaymentMethod> methods = List.of(PaymentMethod.PIX);

        PayableSummaryResponse summary = PayableSummaryResponse.builder()
                .id(1L)
                .originType(PayableOriginType.MANUAL)
                .description("Fornecedor XYZ")
                .amount(BigDecimal.valueOf(150.00))
                .pendingAmount(BigDecimal.valueOf(150.00))
                .status(PayableStatus.PENDING)
                .build();
        Page<PayableSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(payableService.listPayables("Fornecedor", startDate, endDate, statuses, methods, 10L, pageable))
                .thenReturn(page);

        ResponseEntity<Page<PayableSummaryResponse>> response = payableController.listPayables(
                "Fornecedor", startDate, endDate, statuses, methods, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(payableService).listPayables("Fornecedor", startDate, endDate, statuses, methods, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/payables/{payableId} — Deve buscar conta a pagar por ID")
    void shouldFindPayableById() {
        PayableResponse expected = PayableResponse.builder()
                .id(1L)
                .companyId(10L)
                .status(PayableStatus.PAID)
                .build();

        when(payableService.findById(10L, 1L)).thenReturn(expected);

        ResponseEntity<PayableResponse> response = payableController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(payableService).findById(10L, 1L);
    }
}
