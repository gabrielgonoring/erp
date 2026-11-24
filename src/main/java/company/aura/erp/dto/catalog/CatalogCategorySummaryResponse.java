package company.aura.erp.dto.catalog;

import company.aura.erp.enums.CatalogCategoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCategorySummaryResponse {
    private Long id;
    private String name;
    private CatalogCategoryStatus status;
}
