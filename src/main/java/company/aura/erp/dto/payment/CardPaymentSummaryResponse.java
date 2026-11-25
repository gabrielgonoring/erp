package company.aura.erp.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CardPaymentSummaryResponse(
        Period period,
        Summary summary,
        List<CardBrandPaymentSummary> brands
) {
    public record Period(
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record Summary(
            Long count,
            BigDecimal totalAmount
    ) {
        public Summary {
            count = count != null ? count : 0L;
            totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        }
    }
}
