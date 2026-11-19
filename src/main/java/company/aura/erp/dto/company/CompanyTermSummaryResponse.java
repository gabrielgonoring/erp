package company.aura.erp.dto.company;

import company.aura.erp.enums.CompanyTermStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyTermSummaryResponse {
    private Long id;
    private String title;
    private CompanyTermStatus status;
    private Boolean isDefault;
}
