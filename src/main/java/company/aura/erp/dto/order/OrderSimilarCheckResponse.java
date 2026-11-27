package company.aura.erp.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSimilarCheckResponse {
    private boolean hasSimilar;
    private List<OrderSummaryResponse> similarOrders;
}
