package company.aura.erp.dto.collaborator;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorUpdateRequest {

    @NotBlank(message = "O nome do colaborador é obrigatório.")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    private String name;

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Long userId;

    @Size(max = 14, message = "O CPF deve ter no máximo 14 caracteres.")
    private String cpf;

    @Email(message = "Formato de e-mail inválido.")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
    private String email;

    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
    private String phone;

    @DecimalMin(value = "0.0", message = "A porcentagem de comissão não pode ser negativa.")
    @DecimalMax(value = "100.0", message = "A porcentagem de comissão não pode ser maior que 100%.")
    @JsonProperty("default_commission_percentage")
    @JsonAlias({"defaultCommissionPercentage", "commissionPercentage", "commission_percentage", "porcentagemComissao", "comissaoPadrao", "porcentagem_comissao", "comissao_padrao"})
    private BigDecimal defaultCommissionPercentage;

    // Endereço
    @Size(max = 255, message = "O logradouro deve ter no máximo 255 caracteres.")
    private String street;

    @Size(max = 20, message = "O número deve ter no máximo 20 caracteres.")
    private String number;

    @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres.")
    private String complement;

    @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres.")
    private String neighborhood;

    @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres.")
    private String city;

    @Size(max = 2, message = "O estado deve ter 2 caracteres.")
    private String state;

    @Size(max = 8, message = "O CEP deve ter no máximo 8 dígitos.")
    private String zipCode;

    @Size(max = 7, message = "O código IBGE deve ter no máximo 7 dígitos.")
    private String ibgeCityCode;

    // Campos trabalhistas / legais
    @Size(max = 20, message = "O RG deve ter no máximo 20 caracteres.")
    private String rg;

    private LocalDate birthDate;

    private LocalDate admissionDate;

    private LocalDate resignationDate;

    @Size(max = 100, message = "O cargo deve ter no máximo 100 caracteres.")
    private String jobTitle;

    @Size(max = 20, message = "O PIS/PASEP deve ter no máximo 20 caracteres.")
    private String pisPasep;

    public BigDecimal getCommissionPercentage() {
        return this.defaultCommissionPercentage;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.defaultCommissionPercentage = commissionPercentage;
    }
}
