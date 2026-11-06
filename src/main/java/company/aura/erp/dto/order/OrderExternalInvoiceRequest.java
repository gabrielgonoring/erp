package company.aura.erp.dto.order;

import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderExternalInvoiceRequest {
    DocumentType documentType;
    String externalReference;
    Integer operationType;
    BigDecimal totalAmount;

}
