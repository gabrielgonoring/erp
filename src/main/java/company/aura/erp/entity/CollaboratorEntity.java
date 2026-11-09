package company.aura.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "collaborators")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CollaboratorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "cpf", length = 14)
    private String cpf;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 45)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "default_commission_percentage", precision = 5, scale = 2)
    private BigDecimal defaultCommissionPercentage = BigDecimal.ZERO;

    // Endereço
    @Column(name = "street")
    private String street;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "zip_code", length = 8)
    private String zipCode;

    @Column(name = "ibge_city_code", length = 7)
    private String ibgeCityCode;

    // Campos trabalhistas / exigidos por lei (CLT / eSocial)
    @Column(name = "rg", length = 20)
    private String rg;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "resignation_date")
    private LocalDate resignationDate;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "pis_pasep", length = 20)
    private String pisPasep;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
