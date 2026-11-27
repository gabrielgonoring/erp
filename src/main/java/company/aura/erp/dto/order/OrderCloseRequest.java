package company.aura.erp.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCloseRequest {
    private List<@Valid ReceivableRequest> receivables;

    @Valid
    private OrderShippingRequest shipping;

    @DecimalMin(value = "0.00", message = "O valor de desconto não pode ser negativo.")
    private BigDecimal discountAmount;

    private LocalDateTime completedAt;
}
