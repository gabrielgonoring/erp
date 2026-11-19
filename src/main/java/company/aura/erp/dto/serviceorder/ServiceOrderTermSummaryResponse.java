package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.ServiceOrderTermStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderTermSummaryResponse {
    private Long id;
    private String title;
    private ServiceOrderTermStatus status;
    private Boolean isDefault;
}
