package company.aura.erp.dto.catalog;

import company.aura.erp.enums.CatalogCategoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCategoryResponse {
    private Long id;
    private Long companyId;
    private String name;
    private CatalogCategoryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
