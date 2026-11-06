package company.aura.erp.dto.invoice;

import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class InvoiceCreateRequest {
    @NotNull(message = "O ID do parceiro (cliente/destinatário) é obrigatório.")
    private Long partnerId;
    @NotNull(message = "O valor total da nota fiscal é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor total deve ser maior que zero.")
    private BigDecimal totalAmount;
    private DocumentType documentType;
    private EnvironmentType environment;
    private String status;
    private Integer operationType;
    private Integer invoiceNumber;
    private Integer series;
    private String accessKey;
    private String authorizationProtocol;
    private String paymentMethod;
    private LocalDate dueDate;
    private Integer freightMode;
    private BigDecimal freightAmount;
    private BigDecimal additionalExpenses;
    private BigDecimal productsAmount;
    private BigDecimal discountAmount;
    private String additionalInformation;
    private String externalReference;
    private LocalDateTime issuedAt;
}
