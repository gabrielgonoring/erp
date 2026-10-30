package company.aura.erp.controller;

import company.aura.erp.dto.catalog.*;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CatalogItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/catalog-items", "/api/catalog"})
@RequiredArgsConstructor
public class CatalogItemController {

    private final CatalogItemService catalogItemService;

    @PostMapping("/physical")
    public ResponseEntity<CatalogItemResponse> createPhysical(
            @RequestBody @Valid CatalogPhysicalItemCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CatalogItemResponse response = catalogItemService.savePhysical(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/service")
    public ResponseEntity<CatalogItemResponse> createService(
            @RequestBody @Valid CatalogServiceItemCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CatalogItemResponse response = catalogItemService.saveService(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/physical/{id}")
    public ResponseEntity<CatalogItemResponse> updatePhysical(
            @PathVariable Long id,
            @RequestBody @Valid CatalogPhysicalItemUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CatalogItemResponse response = catalogItemService.updatePhysical(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/service/{id}")
    public ResponseEntity<CatalogItemResponse> updateService(
            @PathVariable Long id,
            @RequestBody @Valid CatalogServiceItemUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CatalogItemResponse response = catalogItemService.updateService(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogItemResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CatalogItemResponse response = catalogItemService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CatalogItemSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "description") Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<CatalogItemSummaryResponse> page = catalogItemService.listSummarized(search, userPrincipal.getCompanyId(), pageable);
        return ResponseEntity.ok(page);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        catalogItemService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
