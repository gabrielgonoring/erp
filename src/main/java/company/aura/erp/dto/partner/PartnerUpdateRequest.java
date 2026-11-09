package company.aura.erp.dto.partner;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PartnerUpdateRequest(

        @Size(max = 100, message = "Identificador do parceiro deve ter no máximo 100 caracteres.")
        String partnerIdentifier,

        @NotNull(message = "Informe se o parceiro é um cliente")
        Boolean isCustomer,

        @NotNull(message = "Informe se a pessoa é um fornecedor")
        Boolean isSupplier,

        Boolean isCarrier,

        @Size(min = 11, max = 14, message = "Documento deve conter entre 11 e 14 dígitos numéricos.")
        String document,

        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres.")
        String name,

        Integer indicatorIe,

        @Size(max = 20, message = "Inscrição Estadual deve ter no máximo 20 caracteres.")
        String stateRegistration,

        @Email(message = "Formato de e-mail inválido.")
        @Size(max = 255, message = "E-mail deve ter no máximo 255 caracteres.")
        String email,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        String phone,

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

        @Pattern(regexp = "^$|\\d{8}", message = "CEP deve conter exatamente 8 dígitos numéricos.")
        String zipCode,

        @Size(max = 4, message = "Código do país deve ter no máximo 4 caracteres.")
        String countryCode,

        @Size(max = 50, message = "Nome do país deve ter no máximo 50 caracteres.")
        String countryName
) {
    public PartnerUpdateRequest(
            Boolean isCustomer,
            Boolean isSupplier,
            Boolean isCarrier,
            String document,
            String name,
            Integer indicatorIe,
            String stateRegistration,
            String email,
            String phone,
            String street,
            String number,
            String complement,
            String neighborhood,
            String ibgeCityCode,
            String city,
            String state,
            String zipCode,
            String countryCode,
            String countryName
    ) {
        this(null, isCustomer, isSupplier, isCarrier, document, name, indicatorIe,
                stateRegistration, email, phone, street, number, complement, neighborhood,
                ibgeCityCode, city, state, zipCode, countryCode, countryName);
    }
}
