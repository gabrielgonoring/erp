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

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "partner_identifier", length = 100)
    private String partnerIdentifier;

    @Builder.Default
    @Column(name = "is_customer")
    private Boolean isCustomer = false;

    @Column(name = "status", length = 45)
    private String status;

    @Builder.Default
    @Column(name = "is_supplier")
    private Boolean isSupplier = false;

    @Builder.Default
    @Column(name = "is_carrier", nullable = false)
    private Boolean isCarrier = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 2)
    private PersonType personType;

    @Column(name = "document", length = 14)
    private String document;

    @Column(name = "name", nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "indicator_ie")
    private Integer indicatorIe = 9;

    @Column(name = "state_registration", length = 20)
    private String stateRegistration;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "street")
    private String street;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "ibge_city_code", length = 7)
    private String ibgeCityCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "zip_code", length = 8)
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
