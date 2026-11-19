package company.aura.erp.dto.email;

import company.aura.erp.enums.CompanyEmailConfigStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEmailResponse {
    private Long id;
    private Long companyId;
    private String notificationType;
    private String fromEmail;
    private String fromName;
    private String replyTo;
    private CompanyEmailConfigStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
