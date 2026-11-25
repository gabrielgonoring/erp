package company.aura.erp.service;

import company.aura.erp.dto.payment.CardBrandPaymentSummary;
import company.aura.erp.dto.payment.CardPaymentSummaryResponse;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportPaymentService {

    private static final List<PaymentMethod> CARD_PAYMENT_METHODS = List.of(
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.DEBIT_CARD
    );

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional(readOnly = true)
    public CardPaymentSummaryResponse getCardPaymentSummary(LocalDate startDate, LocalDate endDate, Long companyId) {
        log.info("getCardPaymentSummary() - START - companyId={}, startDate={}, endDate={}", companyId, startDate, endDate);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
        }

        boolean hasStartDate = startDate != null;
        boolean hasEndDate = endDate != null;
        LocalDateTime startDateTime = hasStartDate ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = hasEndDate ? endDate.atTime(LocalTime.MAX) : null;

        List<CardBrandPaymentSummary> rawBrands = paymentTransactionRepository.summarizeCardPaymentsByBrand(
                companyId,
                CARD_PAYMENT_METHODS,
                hasStartDate,
                startDateTime,
                hasEndDate,
                endDateTime
        );

        List<CardBrandPaymentSummary> brands = mergeBrandsCaseInsensitive(rawBrands);

        long totalCount = brands.stream()
                .mapToLong(CardBrandPaymentSummary::count)
                .sum();

        BigDecimal totalAmount = brands.stream()
                .map(CardBrandPaymentSummary::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        CardPaymentSummaryResponse response = new CardPaymentSummaryResponse(
                new CardPaymentSummaryResponse.Period(startDate, endDate),
                new CardPaymentSummaryResponse.Summary(totalCount, totalAmount),
                brands
        );

        log.info("getCardPaymentSummary() - END - companyId={}, brandsCount={}, totalCount={}, totalAmount={}",
                companyId, brands.size(), totalCount, totalAmount);
        return response;
    }

    private List<CardBrandPaymentSummary> mergeBrandsCaseInsensitive(List<CardBrandPaymentSummary> rawBrands) {
        if (rawBrands == null || rawBrands.isEmpty()) {
            return List.of();
        }

        Map<String, CardBrandPaymentSummary> merged = new LinkedHashMap<>();
        for (CardBrandPaymentSummary item : rawBrands) {
            String key = item.cardBrand().toLowerCase(Locale.ROOT);
            merged.merge(key, item, (existing, incoming) -> new CardBrandPaymentSummary(
                    existing.cardBrand(),
                    existing.count() + incoming.count(),
                    existing.totalAmount().add(incoming.totalAmount())
            ));
        }

        return merged.values().stream()
                .map(item -> new CardBrandPaymentSummary(
                        item.cardBrand(),
                        item.count(),
                        item.totalAmount().setScale(2, RoundingMode.HALF_UP)
                ))
                .sorted(Comparator.comparing(CardBrandPaymentSummary::totalAmount).reversed()
                        .thenComparing(CardBrandPaymentSummary::cardBrand, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
