package company.aura.erp.repository;

import company.aura.erp.dto.payment.CardBrandPaymentSummary;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.PaymentTransactionType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class PaymentTransactionRepositoryTest {

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("summarizeCardPaymentsByBrand — Deve agrupar crédito e débito por bandeira, abater estornos e respeitar empresa e datas")
    void shouldSummarizeCardPaymentsByBrandWithDateFilter() {
        Long companyId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 23, 59, 59, 999999999);

        // Visa (crédito + débito - estorno)
        saveTransaction(companyId, PaymentTransactionType.PAYMENT, PaymentMethod.CREDIT_CARD, "Visa", new BigDecimal("300.00"), LocalDateTime.of(2026, 11, 5, 10, 0));
        saveTransaction(companyId, PaymentTransactionType.PAYMENT, PaymentMethod.DEBIT_CARD, "Visa", new BigDecimal("150.00"), LocalDateTime.of(2026, 11, 10, 14, 0));
        saveTransaction(companyId, PaymentTransactionType.REFUND, PaymentMethod.CREDIT_CARD, "Visa", new BigDecimal("50.00"), LocalDateTime.of(2026, 11, 12, 16, 0));

        // Mastercard
        saveTransaction(companyId, PaymentTransactionType.PAYMENT, PaymentMethod.CREDIT_CARD, "Mastercard", new BigDecimal("200.00"), LocalDateTime.of(2026, 11, 15, 11, 0));

        // Cartão sem bandeira informada
        saveTransaction(companyId, PaymentTransactionType.PAYMENT, PaymentMethod.DEBIT_CARD, "   ", new BigDecimal("80.00"), LocalDateTime.of(2026, 11, 18, 9, 0));

        // Devem ser ignorados: PIX, fora do período e outra empresa
        saveTransaction(companyId, PaymentTransactionType.PAYMENT, PaymentMethod.PIX, null, new BigDecimal("999.00"), LocalDateTime.of(2026, 11, 18, 9, 0));
        saveTransaction(companyId, PaymentTransactionType.PAYMENT, PaymentMethod.CREDIT_CARD, "Elo", new BigDecimal("500.00"), LocalDateTime.of(2026, 10, 20, 9, 0));
        saveTransaction(2L, PaymentTransactionType.PAYMENT, PaymentMethod.CREDIT_CARD, "Visa", new BigDecimal("700.00"), LocalDateTime.of(2026, 11, 18, 9, 0));

        entityManager.flush();
        entityManager.clear();

        List<CardBrandPaymentSummary> filtered = paymentTransactionRepository.summarizeCardPaymentsByBrand(
                companyId,
                List.of(PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD),
                true,
                start,
                true,
                end
        );

        assertEquals(3, filtered.size());
        assertEquals("Visa", filtered.get(0).cardBrand());
        assertEquals(2L, filtered.get(0).count());
        assertEquals(0, new BigDecimal("400.00").compareTo(filtered.get(0).totalAmount()));

        assertEquals("Mastercard", filtered.get(1).cardBrand());
        assertEquals(1L, filtered.get(1).count());
        assertEquals(0, new BigDecimal("200.00").compareTo(filtered.get(1).totalAmount()));

        assertEquals("Não informada", filtered.get(2).cardBrand());
        assertEquals(1L, filtered.get(2).count());
        assertEquals(0, new BigDecimal("80.00").compareTo(filtered.get(2).totalAmount()));

        // Sem filtro de datas deve incluir Elo de outubro também
        List<CardBrandPaymentSummary> unfiltered = paymentTransactionRepository.summarizeCardPaymentsByBrand(
                companyId,
                List.of(PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD),
                false,
                null,
                false,
                null
        );
        assertEquals(4, unfiltered.size());
        assertEquals("Elo", unfiltered.get(0).cardBrand());
        assertEquals(0, new BigDecimal("500.00").compareTo(unfiltered.get(0).totalAmount()));
    }

    private void saveTransaction(
            Long companyId,
            PaymentTransactionType type,
            PaymentMethod method,
            String cardBrand,
            BigDecimal amount,
            LocalDateTime paidAt
    ) {
        PaymentTransactionEntity entity = paymentTransactionRepository.saveAndFlush(
                PaymentTransactionEntity.builder()
                        .receivableId(1L)
                        .companyId(companyId)
                        .transactionType(type)
                        .amountPaid(amount)
                        .paymentMethod(method)
                        .cardBrand(cardBrand)
                        .build()
        );
        entityManager.createNativeQuery("UPDATE payment_transactions SET paid_at = :paidAt WHERE id = :id")
                .setParameter("paidAt", paidAt)
                .setParameter("id", entity.getId())
                .executeUpdate();
    }
}
