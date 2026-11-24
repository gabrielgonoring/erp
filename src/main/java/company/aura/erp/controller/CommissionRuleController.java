package company.aura.erp.controller;

import company.aura.erp.dto.commission.CommissionRuleCreateRequest;
import company.aura.erp.dto.commission.CommissionRuleResponse;
import company.aura.erp.dto.commission.CommissionRuleSummaryResponse;
import company.aura.erp.dto.commission.CommissionRuleUpdateRequest;
import company.aura.erp.enums.CommissionRuleStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CommissionRuleService;
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
@RequestMapping("/api/commission-rules")
@RequiredArgsConstructor
public class CommissionRuleController {

    private final CommissionRuleService commissionRuleService;

    @PostMapping
    public ResponseEntity<CommissionRuleResponse> create(
            @RequestBody @Valid CommissionRuleCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CommissionRuleResponse response = commissionRuleService.create(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommissionRuleResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid CommissionRuleUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CommissionRuleResponse response = commissionRuleService.update(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        commissionRuleService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommissionRuleResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CommissionRuleResponse response = commissionRuleService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CommissionRuleSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long collaboratorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long catalogItemId,
            @RequestParam(required = false) CommissionRuleStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<CommissionRuleSummaryResponse> page = commissionRuleService.listSummarized(
                search,
                collaboratorId,
                categoryId,
                catalogItemId,
                status,
                userPrincipal.getCompanyId(),
                pageable
        );
        return ResponseEntity.ok(page);
    }
}
