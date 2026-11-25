package company.aura.erp.service;

import company.aura.erp.dto.payment.CardBrandPaymentSummary;
import company.aura.erp.dto.payment.CardPaymentSummaryResponse;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportPaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private ReportPaymentService reportPaymentService;

    private final Long companyId = 10L;

    @Test
    @DisplayName("Deve consolidar pagamentos por bandeira (mesclando maiúsculas/minúsculas) e retornar compilado geral")
    void shouldAggregateCardPaymentsByBrandAndOverallSummary() {
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 25);
        LocalDateTime expectedStart = startDate.atStartOfDay();
        LocalDateTime expectedEnd = endDate.atTime(LocalTime.MAX);

        when(paymentTransactionRepository.summarizeCardPaymentsByBrand(
                eq(companyId),
                eq(List.of(PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD)),
                eq(true),
                eq(expectedStart),
                eq(true),
                eq(expectedEnd)
        )).thenReturn(List.of(
                new CardBrandPaymentSummary("Visa", 3L, new BigDecimal("350.5")),
                new CardBrandPaymentSummary("visa", 1L, new BigDecimal("49.50")),
                new CardBrandPaymentSummary("Mastercard", 2L, new BigDecimal("250.00"))
        ));

        CardPaymentSummaryResponse response = reportPaymentService.getCardPaymentSummary(startDate, endDate, companyId);

        assertNotNull(response);
        assertEquals(startDate, response.period().startDate());
        assertEquals(endDate, response.period().endDate());
        assertEquals(6L, response.summary().count());
        assertEquals(new BigDecimal("650.00"), response.summary().totalAmount());
        assertEquals(2, response.brands().size());
        assertEquals("Visa", response.brands().get(0).cardBrand());
        assertEquals(4L, response.brands().get(0).count());
        assertEquals(new BigDecimal("400.00"), response.brands().get(0).totalAmount());
        assertEquals("Mastercard", response.brands().get(1).cardBrand());
        assertEquals(2L, response.brands().get(1).count());
        assertEquals(new BigDecimal("250.00"), response.brands().get(1).totalAmount());
    }

    @Test
    @DisplayName("Deve permitir consulta sem intervalo de datas e retornar compilado zerado quando não houver pagamentos")
    void shouldReturnZeroedSummaryWhenNoCardPaymentsExist() {
        when(paymentTransactionRepository.summarizeCardPaymentsByBrand(
                eq(companyId),
                eq(List.of(PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD)),
                eq(false),
                isNull(),
                eq(false),
                isNull()
        )).thenReturn(Collections.emptyList());

        CardPaymentSummaryResponse response = reportPaymentService.getCardPaymentSummary(null, null, companyId);

        assertNotNull(response);
        assertNull(response.period().startDate());
        assertNull(response.period().endDate());
        assertEquals(0L, response.summary().count());
        assertEquals(new BigDecimal("0.00"), response.summary().totalAmount());
        assertTrue(response.brands().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar HTTP 400 quando startDate for posterior a endDate")
    void shouldThrowBadRequestWhenStartDateIsAfterEndDate() {
        LocalDate startDate = LocalDate.of(2026, 11, 30);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reportPaymentService.getCardPaymentSummary(startDate, endDate, companyId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(paymentTransactionRepository);
    }
}
