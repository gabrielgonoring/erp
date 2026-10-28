package company.aura.erp.controller;

import company.aura.erp.dto.partner.PartnerRequest;
import company.aura.erp.dto.partner.PartnerResponse;
import company.aura.erp.dto.partner.PartnerSummaryResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping
    public ResponseEntity<PartnerResponse> create(
            @RequestBody PartnerRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        validateCompany(userPrincipal);
        PartnerResponse response = partnerService.save(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartnerResponse> update(
            @PathVariable Long id,
            @RequestBody PartnerRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        validateCompany(userPrincipal);
        PartnerResponse response = partnerService.update(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        validateCompany(userPrincipal);
        PartnerResponse response = partnerService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<PartnerSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        validateCompany(userPrincipal);
        Page<PartnerSummaryResponse> page = partnerService.listSummarized(search, userPrincipal.getCompanyId(), pageable);
        return ResponseEntity.ok(page);
    }

    private void validateCompany(UserPrincipal userPrincipal) {
        if (userPrincipal == null || userPrincipal.getCompanyId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não possui uma empresa vinculada.");
        }
    }
}
