package company.aura.erp.dto.payable;

import company.aura.erp.enums.PayableOriginType;
import company.aura.erp.enums.PayableStatus;
import company.aura.erp.enums.PaymentMethod;
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
public class PayableSummaryResponse {
    private Long id;
    private PayableOriginType originType;
    private Long originId;
    private String description;
    private Long partnerId;
    private String partnerDocument;
    private String partnerName;
    private Long collaboratorId;
    private String collaboratorDocument;
    private String collaboratorName;
    private BigDecimal amount;
    private BigDecimal pendingAmount;
    private PayableStatus status;
    private PaymentMethod paymentMethod;
    private LocalDate dueDate;
}
