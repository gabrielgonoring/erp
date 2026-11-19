package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.ServiceOrderUpdateEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderUpdateResponse {
    private Long id;
    private Long companyId;
    private Long serviceOrderId;
    private ServiceOrderUpdateEventType eventType;
    private String description;
    private LocalDateTime createdAt;
}
