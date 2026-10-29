package company.aura.erp.dto.company;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CompanyUpdateRequest(

        @Size(max = 255, message = "Razão social deve ter no máximo 255 caracteres.")
        String corporateName,

        @Size(max = 255, message = "Nome fantasia deve ter no máximo 255 caracteres.")
        String tradeName,

        @Size(max = 20, message = "Inscrição Estadual deve ter no máximo 20 caracteres.")
        String stateRegistration,

        @Size(max = 20, message = "Inscrição Municipal deve ter no máximo 20 caracteres.")
        String municipalRegistration,

        Integer taxRegime,

        @Size(max = 255, message = "Logradouro deve ter no máximo 255 caracteres.")
        String street,

        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres.")
        String number,

        @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres.")
        String complement,

        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres.")
        String neighborhood,

        @Size(max = 7, message = "Código IBGE deve ter no máximo 7 caracteres.")
        String ibgeCityCode,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.")
        String city,

        @Size(min = 2, max = 2, message = "Estado (UF) deve ter exatamente 2 caracteres.")
        String state,

        @Pattern(regexp = "\\d{8}", message = "CEP deve conter exatamente 8 dígitos numéricos.")
        String zipCode,

        @Size(max = 4, message = "Código do país deve ter no máximo 4 caracteres.")
        String countryCode,

        @Size(max = 50, message = "Nome do país deve ter no máximo 50 caracteres.")
        String countryName
) {}
