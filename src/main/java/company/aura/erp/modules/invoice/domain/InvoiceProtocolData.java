package company.aura.erp.modules.invoice.domain;

import company.aura.erp.enums.EnvironmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceProtocolData {
    private EnvironmentType environment;
    private String applicationVersion;
    private String accessKey;
    private LocalDateTime authorizedAt;
    private String authorizationProtocol;
    private String digestValue;
    private String statusCode;
    private String statusReason;
    private boolean authorized;
}
