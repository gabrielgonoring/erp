package company.aura.erp.modules.invoice.domain;

import company.aura.erp.modules.invoice.enumerate.FreightMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFreightData {
    @Builder.Default
    private Integer freightModeCode = 9;
    @Builder.Default
    private FreightMode freightMode = FreightMode.NO_FREIGHT;
    private InvoiceCarrierData carrier;
    private String vehiclePlate;
    private String vehicleState;
    private String rntrcCode;

    @Builder.Default
    private List<InvoiceVolumeData> volumes = new ArrayList<>();
}
