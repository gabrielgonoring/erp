package company.aura.erp.modules.invoice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceVolumeData {
    private Integer quantity;
    private String species;
    private String brand;
    private String numbering;
    private BigDecimal netWeightKg;
    private BigDecimal grossWeightKg;
}
