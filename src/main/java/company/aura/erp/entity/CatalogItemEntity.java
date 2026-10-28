package company.aura.erp.entity;

import company.aura.erp.enums.CatalogItemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CatalogItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private CatalogItemType type;

    @Column(name = "internal_code", length = 50)
    private String internalCode;

    @Column(name = "status", length = 45)
    private String status;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "unit_of_measure", nullable = false, length = 10)
    private String unitOfMeasure;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "catalogItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private CatalogPhysicalItemEntity physicalItem;

    @OneToOne(mappedBy = "catalogItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private CatalogServiceItemEntity serviceItem;
}
