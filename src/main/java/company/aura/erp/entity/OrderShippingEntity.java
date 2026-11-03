package company.aura.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_shipping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class OrderShippingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "carrier_id")
    private Long carrierId;

    @Builder.Default
    @Column(name = "freight_mode", nullable = false)
    private Integer freightMode = 0;

    @Column(name = "vehicle_plate", length = 8)
    private String vehiclePlate;

    @Column(name = "vehicle_state", length = 2)
    private String vehicleState;

    @Column(name = "rntrc_code", length = 20)
    private String rntrcCode;

    @Builder.Default
    @Column(name = "package_quantity", nullable = false)
    private Integer packageQuantity = 1;

    @Builder.Default
    @Column(name = "package_species", length = 50)
    private String packageSpecies = "CAIXA";

    @Column(name = "package_brand", length = 50)
    private String packageBrand;

    @Column(name = "package_numbering", length = 50)
    private String packageNumbering;

    @Builder.Default
    @Column(name = "gross_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal grossWeightKg = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal netWeightKg = BigDecimal.ZERO;

    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "shipping_notes", length = 255)
    private String shippingNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
