package company.aura.erp.dto.partner;

import company.aura.erp.enums.PersonType;
import jakarta.validation.constraints.*;

public record PartnerCreateRequest(

        @NotNull(message = "Informe se o parceiro é um cliente")
        Boolean isCustomer,

        @NotNull(message = "Informe se a pessoa é um fornecedor")
        Boolean isSupplier,

        @NotNull(message = "Tipo de pessoa é obrigatório.")
        PersonType personType,

        @NotBlank(message = "Documento (CPF ou CNPJ) é obrigatório.")
        @Size(min = 11, max = 14, message = "Documento deve conter entre 11 e 14 dígitos numéricos.")
        String document,

        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres.")
        String name,

        Integer indicatorIe,

        @Size(max = 20, message = "Inscrição Estadual deve ter no máximo 20 caracteres.")
        String stateRegistration,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        @Size(max = 255, message = "E-mail deve ter no máximo 255 caracteres.")
        String email,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        String phone,

        @NotBlank(message = "Logradouro é obrigatório.")
        @Size(max = 255, message = "Logradouro deve ter no máximo 255 caracteres.")
        String street,

        @NotBlank(message = "Número é obrigatório.")
        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres.")
        String number,

        @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres.")
        String complement,

        @NotBlank(message = "Bairro é obrigatório.")
        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres.")
        String neighborhood,

        @NotBlank(message = "Código IBGE da cidade é obrigatório.")
        @Size(max = 7, message = "Código IBGE deve ter no máximo 7 caracteres.")
        String ibgeCityCode,

        @NotBlank(message = "Cidade é obrigatória.")
        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.")
        String city,

        @NotBlank(message = "Estado (UF) é obrigatório.")
        @Size(min = 2, max = 2, message = "Estado (UF) deve ter exatamente 2 caracteres.")
        String state,

        @NotBlank(message = "CEP é obrigatório.")
        @Pattern(regexp = "\\d{8}", message = "CEP deve conter exatamente 8 dígitos numéricos.")
        String zipCode,

        @Size(max = 4, message = "Código do país deve ter no máximo 4 caracteres.")
        String countryCode,

        @Size(max = 50, message = "Nome do país deve ter no máximo 50 caracteres.")
        String countryName
) {}
