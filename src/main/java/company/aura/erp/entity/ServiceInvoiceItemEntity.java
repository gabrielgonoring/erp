package company.aura.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "service_invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "invoiceItemId")
public class ServiceInvoiceItemEntity {

    @Id
    @Column(name = "invoice_item_id")
    private Long invoiceItemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "invoice_item_id")
    private InvoiceItemEntity invoiceItem;

    @Column(name = "lc116_service_code", nullable = false, length = 10)
    private String lc116ServiceCode;

    @Column(name = "city_tax_code", length = 20)
    private String cityTaxCode;

    @Column(name = "service_execution_city_ibge", length = 7)
    private String serviceExecutionCityIbge;

    @Builder.Default
    @Column(name = "iss_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal issRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "iss_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal issAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "iss_withheld")
    private Boolean issWithheld = false;

    @Builder.Default
    @Column(name = "deductions_amount", precision = 12, scale = 2)
    private BigDecimal deductionsAmount = BigDecimal.ZERO;
}
