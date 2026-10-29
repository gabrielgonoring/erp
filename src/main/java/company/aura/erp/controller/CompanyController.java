package company.aura.erp.controller;

import company.aura.erp.dto.company.CompanyCreateRequest;
import company.aura.erp.dto.company.CompanyResponse;
import company.aura.erp.dto.company.CompanyUpdateRequest;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyResponse> create(
            @RequestBody @Valid CompanyCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CompanyResponse response = companyService.save(request, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<CompanyResponse> update(
            @RequestBody @Valid CompanyUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CompanyResponse response = companyService.update(userPrincipal, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<CompanyResponse> findById(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        CompanyResponse response = companyService.findById(userPrincipal);
        return ResponseEntity.ok(response);
    }
}
