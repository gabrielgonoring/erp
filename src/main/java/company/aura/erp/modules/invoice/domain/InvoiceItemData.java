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
public class InvoiceItemData {
    private Integer itemNumber;
    private String productCode;
    private String gtinEan;
    private String taxableGtinEan;
    private String description;
    private String ncm;
    private String cest;
    private String cfop;
    private String unitOfMeasure;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String taxableUnitOfMeasure;
    private BigDecimal taxableQuantity;
    private BigDecimal taxableUnitPrice;
    private BigDecimal freightAmount;
    private BigDecimal insuranceAmount;
    private BigDecimal discountAmount;
    private BigDecimal otherExpensesAmount;
    private BigDecimal netTotalPrice;
    private Integer totalIndicator;
    private String purchaseOrderNumber;
    private Integer purchaseOrderItemNumber;
    private String additionalInfo;
    private InvoiceItemTaxData taxes;
}
