package company.aura.erp.dto.serviceorder;

import company.aura.erp.enums.ServiceOrderTermStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderTermResponse {
    private Long id;
    private Long companyId;
    private String title;
    private String content;
    private Boolean isDefault;
    private ServiceOrderTermStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
