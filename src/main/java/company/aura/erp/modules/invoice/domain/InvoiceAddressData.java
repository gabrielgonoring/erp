package company.aura.erp.modules.invoice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceAddressData {
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String ibgeCityCode;
    private String city;
    private String state;
    private String zipCode;
    private String countryCode;
    private String countryName;
    private String phone;
}
