package company.aura.erp.dto.order;

import company.aura.erp.enums.CatalogItemType;
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
public class OrderItemResponse {
    private Long id;
    private Long orderId;
    private Long catalogItemId;
    private CatalogItemType itemType;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private Long professionalId;
    private Boolean stockDeducted;
    private LocalDateTime createdAt;
}
