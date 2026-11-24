package company.aura.erp.dto.order;

import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableOriginType;
import company.aura.erp.enums.ReceivableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivableSummaryResponse {
    private Long id;
    private Long orderId;
    private Long serviceOrderId;
    private ReceivableOriginType originType;
    private Long partnerId;
    private String customerDocument;
    private String customerName;
    private BigDecimal amount;
    private BigDecimal pendingAmount;
    private ReceivableStatus status;
    private PaymentMethod paymentMethod;
    private LocalDate dueDate;
}
