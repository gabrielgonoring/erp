package company.aura.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "physical_invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "invoiceItemId")
public class PhysicalInvoiceItemEntity {

    @Id
    @Column(name = "invoice_item_id")
    private Long invoiceItemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "invoice_item_id")
    private InvoiceItemEntity invoiceItem;

    @Column(name = "ncm", nullable = false, length = 8)
    private String ncm;

    @Column(name = "cfop", nullable = false, length = 4)
    private String cfop;

    @Column(name = "csosn", nullable = false, length = 4)
    private String csosn;

    @Column(name = "cest", length = 7)
    private String cest;

    @Builder.Default
    @Column(name = "gtin_ean", length = 14)
    private String gtinEan = "SEM GTIN";

    @Builder.Default
    @Column(name = "origin")
    private Integer origin = 0;

    @Builder.Default
    @Column(name = "icms_credit_rate", precision = 5, scale = 2)
    private BigDecimal icmsCreditRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "icms_credit_amount", precision = 12, scale = 2)
    private BigDecimal icmsCreditAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "pis_cst", length = 2)
    private String pisCst = "49";

    @Builder.Default
    @Column(name = "cofins_cst", length = 2)
    private String cofinsCst = "49";
}
