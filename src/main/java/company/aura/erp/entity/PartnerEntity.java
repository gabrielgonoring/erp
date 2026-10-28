package company.aura.erp.entity;

import company.aura.erp.enums.PersonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PartnerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Builder.Default
    @Column(name = "is_customer")
    private Boolean isCustomer = false;

    @Column(name = "status", length = 45)
    private String status;

    @Builder.Default
    @Column(name = "is_supplier")
    private Boolean isSupplier = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 2)
    private PersonType personType;

    @Column(name = "document", nullable = false, length = 14)
    private String document;

    @Column(name = "name", nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "indicator_ie")
    private Integer indicatorIe = 9;

    @Column(name = "state_registration", length = 20)
    private String stateRegistration;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "neighborhood", nullable = false, length = 100)
    private String neighborhood;

    @Column(name = "ibge_city_code", nullable = false, length = 7)
    private String ibgeCityCode;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 8)
    private String zipCode;

    @Builder.Default
    @Column(name = "country_code", length = 4)
    private String countryCode = "1058";

    @Builder.Default
    @Column(name = "country_name", length = 50)
    private String countryName = "Brasil";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
