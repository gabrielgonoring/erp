package company.aura.erp.dto.serviceorder;

import company.aura.erp.dto.order.OrderResponse;
import company.aura.erp.enums.ServiceOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderResponse {
    private Long id;
    private Long companyId;
    private Integer orderNumber;
    private Long partnerId;
    private String customerName;
    private Long technicianId;
    private String technicianName;
    private Long orderId;
    private ServiceOrderStatus status;
    private String objectTitle;
    private String objectIdentifier;
    private String entryDetails;
    private String reportedIssue;
    private String technicalReport;
    private String internalNotes;
    private BigDecimal servicesAmount;
    private BigDecimal productsAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private LocalDateTime entryAt;
    private LocalDateTime expectedDeliveryAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ServiceOrderItemResponse> items;
}
