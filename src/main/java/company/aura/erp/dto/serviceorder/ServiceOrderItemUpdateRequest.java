package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.ServiceOrderItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderItemUpdateRequest {

    @NotNull(message = "É preciso relacionar um item do catalogo ao item da ordem de serviço")
    private Long catalogItemId;

    private ServiceOrderItemStatus status;

    @NotBlank(message = "A descrição do item é obrigatória.")
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
    private String description;

    @NotNull(message = "A quantidade é obrigatória.")
    @DecimalMin(value = "0.0001", message = "A quantidade deve ser maior que zero.")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "O preço unitário não pode ser negativo.")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.00", message = "O valor de desconto não pode ser negativo.")
    private BigDecimal discountAmount;

    private Long professionalId;

    private Boolean stockReserved;
}
