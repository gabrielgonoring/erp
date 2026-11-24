package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.ServiceOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderSummaryResponse {
    private Long id;
    private Long partnerId;
    private String customerDocument;
    private String customerName;
    private Long technicianId;
    private String technicianName;
    private Long orderId;
    private String objectTitle;
    private String objectIdentifier;
    private BigDecimal totalAmount;
    private ServiceOrderStatus status;
    private LocalDateTime entryAt;
    private LocalDateTime expectedDeliveryAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
