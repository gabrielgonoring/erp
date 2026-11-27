package company.aura.erp.modules.invoice.domain;

import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceIdentificationData {
    private String accessKey;
    private DocumentType documentType;
    private String modelCode;
    private EnvironmentType environment;
    private Integer invoiceNumber;
    private Integer series;
    private String numericCode;
    private Integer operationType;
    private String operationNature;
    private Integer destinationIndicator;
    private Integer purposeCode;
    private Integer finalConsumerIndicator;
    private Integer presenceIndicator;
    private Integer emissionType;
    private Integer printFormat;
    private String issuingCityIbgeCode;
    private LocalDateTime issuedAt;
    private LocalDateTime exitOrEntryAt;

    @Builder.Default
    private List<String> referencedAccessKeys = new ArrayList<>();
}
