package company.aura.erp.dto.invoice;

import company.aura.erp.dto.catalog.CatalogItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCatalogItemsImportResponse {
    private Long invoiceId;
    private int totalItemsInXml;
    private int createdCount;
    private int existingCount;
    private int skippedCount;

    @Builder.Default
    private List<CatalogItemResponse> createdItems = new ArrayList<>();

    @Builder.Default
    private List<CatalogItemResponse> existingItems = new ArrayList<>();

    @Builder.Default
    private List<InvoiceSkippedCatalogItemResponse> skippedItems = new ArrayList<>();
}
