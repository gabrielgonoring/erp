package company.aura.erp.dto.collaborator;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.mapper.CollaboratorMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorResponse {

    private Long id;
    private Long companyId;

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Long userId;

    private String name;
    private String cpf;
    private String email;
    private String phone;
    private String status;

    @JsonProperty("default_commission_percentage")
    @JsonAlias({"defaultCommissionPercentage", "commissionPercentage", "commission_percentage"})
    private BigDecimal defaultCommissionPercentage;

    // Endereço
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;
    private String zipCode;
    private String ibgeCityCode;

    // Campos trabalhistas / legais
    private String rg;
    private LocalDate birthDate;
    private LocalDate admissionDate;
    private LocalDate resignationDate;
    private String jobTitle;
    private String pisPasep;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("defaultCommissionPercentage")
    public BigDecimal getDefaultCommissionPercentageCamel() {
        return this.defaultCommissionPercentage;
    }

    @JsonProperty("commissionPercentage")
    public BigDecimal getCommissionPercentage() {
        return this.defaultCommissionPercentage;
    }

    @JsonProperty("commission_percentage")
    public BigDecimal getCommissionPercentageSnake() {
        return this.defaultCommissionPercentage;
    }

    public static CollaboratorResponse fromEntity(CollaboratorEntity entity) {
        return CollaboratorMapper.toResponse(entity);
    }
}
