package company.aura.erp.controller;

import company.aura.erp.dto.email.CompanyEmailCreateRequest;
import company.aura.erp.dto.email.CompanyEmailResponse;
import company.aura.erp.dto.email.CompanyEmailUpdateRequest;
import company.aura.erp.enums.CompanyEmailConfigStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CompanyEmailService;
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
@RequestMapping("/api/company/emails")
@RequiredArgsConstructor
public class CompanyEmailController {

    private final CompanyEmailService companyEmailService;

    @PostMapping
    public ResponseEntity<CompanyEmailResponse> create(
            @RequestBody @Valid CompanyEmailCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CompanyEmailResponse response = companyEmailService.create(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CompanyEmailResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) CompanyEmailConfigStatus status,
            @PageableDefault(size = 20, sort = "notificationType", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<CompanyEmailResponse> page = companyEmailService.list(
                search, notificationType, status, userPrincipal.getCompanyId(), pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyEmailResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CompanyEmailResponse response = companyEmailService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyEmailResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid CompanyEmailUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CompanyEmailResponse response = companyEmailService.update(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        companyEmailService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
