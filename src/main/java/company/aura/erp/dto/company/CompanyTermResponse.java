package company.aura.erp.dto.company;

import company.aura.erp.enums.CompanyTermStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyTermResponse {
    private Long id;
    private Long companyId;
    private String title;
    private String content;
    private Boolean isDefault;
    private CompanyTermStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
