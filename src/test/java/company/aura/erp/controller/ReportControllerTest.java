package company.aura.erp.controller;

import company.aura.erp.dto.payment.CardBrandPaymentSummary;
import company.aura.erp.dto.payment.CardPaymentSummaryResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.ReportPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportPaymentService reportPaymentService;

    @InjectMocks
    private ReportController reportController;

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
    @DisplayName("GET /api/reports/card-payments — Deve retornar compilado geral e por bandeira com filtro de datas")
    void shouldReturnCardPaymentsReportWithDateRange() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        CardPaymentSummaryResponse expectedResponse = new CardPaymentSummaryResponse(
                new CardPaymentSummaryResponse.Period(startDate, endDate),
                new CardPaymentSummaryResponse.Summary(5L, new BigDecimal("750.00")),
                List.of(
                        new CardBrandPaymentSummary("Visa", 3L, new BigDecimal("500.00")),
                        new CardBrandPaymentSummary("Mastercard", 2L, new BigDecimal("250.00"))
                )
        );

        when(reportPaymentService.getCardPaymentSummary(startDate, endDate, 10L)).thenReturn(expectedResponse);

        ResponseEntity<CardPaymentSummaryResponse> response = reportController.getCardPaymentsReport(startDate, endDate, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        verify(reportPaymentService).getCardPaymentSummary(startDate, endDate, 10L);
    }

    @Test
    @DisplayName("GET /api/reports/card-payments — Deve retornar compilado sem filtro de datas quando omitidas")
    void shouldReturnCardPaymentsReportWithoutDateRange() {
        CardPaymentSummaryResponse expectedResponse = new CardPaymentSummaryResponse(
                new CardPaymentSummaryResponse.Period(null, null),
                new CardPaymentSummaryResponse.Summary(0L, new BigDecimal("0.00")),
                Collections.emptyList()
        );

        when(reportPaymentService.getCardPaymentSummary(null, null, 10L)).thenReturn(expectedResponse);

        ResponseEntity<CardPaymentSummaryResponse> response = reportController.getCardPaymentsReport(null, null, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(reportPaymentService).getCardPaymentSummary(null, null, 10L);
    }
}
