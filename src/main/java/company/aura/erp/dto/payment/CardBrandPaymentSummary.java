package company.aura.erp.dto.payment;

import java.math.BigDecimal;

public record CardBrandPaymentSummary(
        String cardBrand,
        Long count,
        BigDecimal totalAmount
) {
    public CardBrandPaymentSummary {
        cardBrand = (cardBrand == null || cardBrand.isBlank()) ? "Não informada" : cardBrand.trim();
        count = count != null ? count : 0L;
        totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }
}
