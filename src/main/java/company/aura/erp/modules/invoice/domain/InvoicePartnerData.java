package company.aura.erp.modules.invoice.domain;

import company.aura.erp.enums.PersonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePartnerData {
    private PersonType personType;
    private String document;
    private String foreignId;
    private String name;
    private String tradeName;
    private Integer indicatorIe;
    private String stateRegistration;
    private String municipalRegistration;
    private String cnae;
    private Integer taxRegime;
    private String email;
    private String phone;
    private InvoiceAddressData address;
}
