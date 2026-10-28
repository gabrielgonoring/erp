package company.aura.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "corporate_name", nullable = false)
    private String corporateName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "cnpj", nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(name = "status", length = 45)
    private String status;

    @Column(name = "state_registration", length = 20)
    private String stateRegistration;

    @Column(name = "municipal_registration", length = 20)
    private String municipalRegistration;

    @Builder.Default
    @Column(name = "tax_regime", nullable = false)
    private Integer taxRegime = 1;

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

    @Column(name = "certificate_expiration_date")
    private LocalDateTime certificateExpirationDate;

    @Column(name = "notaas_company_id", length = 100)
    private String notaasCompanyId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
