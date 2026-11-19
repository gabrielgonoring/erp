package company.aura.erp.controller;

import company.aura.erp.dto.company.CompanyTermCreateRequest;
import company.aura.erp.dto.company.CompanyTermResponse;
import company.aura.erp.dto.company.CompanyTermSummaryResponse;
import company.aura.erp.dto.company.CompanyTermUpdateRequest;
import company.aura.erp.enums.CompanyTermStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CompanyTermService;
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
@RequestMapping("/api/company/terms")
@RequiredArgsConstructor
public class CompanyTermsController {

    private final CompanyTermService companyTermService;

    @GetMapping
    public ResponseEntity<Page<CompanyTermSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CompanyTermStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<CompanyTermSummaryResponse> page = companyTermService.listSummarized(
                search,
                status,
                userPrincipal.getCompanyId(),
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyTermResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CompanyTermResponse response = companyTermService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CompanyTermResponse> create(
            @RequestBody @Valid CompanyTermCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CompanyTermResponse response = companyTermService.create(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyTermResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid CompanyTermUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CompanyTermResponse response = companyTermService.update(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        companyTermService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
