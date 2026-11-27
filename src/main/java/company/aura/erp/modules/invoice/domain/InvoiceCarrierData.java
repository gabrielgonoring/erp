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
public class InvoiceCarrierData {
    private PersonType personType;
    private String document;
    private String name;
    private String stateRegistration;
    private String address;
    private String city;
    private String state;
}
