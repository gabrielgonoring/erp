package company.aura.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "catalog_physical_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "catalogItemId")
public class CatalogPhysicalItemEntity {

    @Id
    @Column(name = "catalog_item_id")
    private Long catalogItemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "catalog_item_id")
    private CatalogItemEntity catalogItem;

    @Column(name = "ncm", nullable = false, length = 8)
    private String ncm;

    @Column(name = "default_cfop", nullable = false, length = 4)
    private String defaultCfop;

    @Builder.Default
    @Column(name = "default_csosn", nullable = false, length = 4)
    private String defaultCsosn = "102";

    @Builder.Default
    @Column(name = "gtin_ean", length = 14)
    private String gtinEan = "SEM GTIN";

    @Column(name = "cest", length = 7)
    private String cest;

    @Builder.Default
    @Column(name = "origin")
    private Integer origin = 0;

    @Builder.Default
    @Column(name = "default_icms_credit_rate", precision = 5, scale = 2)
    private BigDecimal defaultIcmsCreditRate = BigDecimal.ZERO;

    @Column(name = "icms_rate", precision = 5, scale = 2)
    private BigDecimal icmsRate;

    @Column(name = "pis_rate", precision = 5, scale = 2)
    private BigDecimal pisRate;

    @Column(name = "cofins_rate", precision = 5, scale = 2)
    private BigDecimal cofinsRate;
}
