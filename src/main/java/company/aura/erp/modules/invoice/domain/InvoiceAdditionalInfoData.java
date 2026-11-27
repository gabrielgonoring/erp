package company.aura.erp.modules.invoice.domain;

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
public class InvoiceAdditionalInfoData {
    private String fiscoInfo;
    private String taxpayerInfo;

    @Builder.Default
    private List<InvoiceObservationData> taxpayerObservations = new ArrayList<>();

    @Builder.Default
    private List<InvoiceObservationData> fiscoObservations = new ArrayList<>();
}
