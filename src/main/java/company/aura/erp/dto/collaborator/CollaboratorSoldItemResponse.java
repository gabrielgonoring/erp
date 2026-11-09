package company.aura.erp.dto.collaborator;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CollaboratorSoldItemResponse {

    private Long itemId;

    private Long originalOrderId;

    private BigDecimal unitPrice;

    private BigDecimal commissionAmount;

    private LocalDateTime date;

    private String description;

    private BigDecimal quantity;

    private BigDecimal totalPrice;

}
