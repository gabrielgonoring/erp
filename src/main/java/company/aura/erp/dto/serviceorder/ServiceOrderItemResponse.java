package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.ServiceOrderItemStatus;
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
public class ServiceOrderItemResponse {
    private Long id;
    private Long serviceOrderId;
    private Long catalogItemId;
    private CatalogItemType itemType;
    private ServiceOrderItemStatus status;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private Long professionalId;
    private Boolean stockReserved;
    private LocalDateTime createdAt;
}
