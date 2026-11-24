package company.aura.erp.controller;

import company.aura.erp.dto.catalog.CatalogCategoryCreateRequest;
import company.aura.erp.dto.catalog.CatalogCategoryResponse;
import company.aura.erp.dto.catalog.CatalogCategorySummaryResponse;
import company.aura.erp.dto.catalog.CatalogCategoryUpdateRequest;
import company.aura.erp.enums.CatalogCategoryStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CatalogCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
public class CatalogCategoryController {

    private final CatalogCategoryService catalogCategoryService;

    @PostMapping
    public ResponseEntity<CatalogCategoryResponse> create(
            @RequestBody @Valid CatalogCategoryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CatalogCategoryResponse response = catalogCategoryService.create(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CatalogCategoryResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid CatalogCategoryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CatalogCategoryResponse response = catalogCategoryService.update(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        catalogCategoryService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogCategoryResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CatalogCategoryResponse response = catalogCategoryService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CatalogCategorySummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CatalogCategoryStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<CatalogCategorySummaryResponse> page = catalogCategoryService.listSummarized(
                search,
                status,
                userPrincipal.getCompanyId(),
                pageable
        );
        return ResponseEntity.ok(page);
    }
}
