package company.aura.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "catalog_service_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "catalogItemId")
public class CatalogServiceItemEntity {

    @Id
    @Column(name = "catalog_item_id")
    private Long catalogItemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "catalog_item_id")
    private CatalogItemEntity catalogItem;

    @Column(name = "lc116_service_code", nullable = false, length = 10)
    private String lc116ServiceCode;

    @Column(name = "city_tax_code", length = 20)
    private String cityTaxCode;

    @Builder.Default
    @Column(name = "iss_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal issRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "iss_withheld")
    private Boolean issWithheld = false;
}
