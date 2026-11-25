package company.aura.erp.controller;

import company.aura.erp.dto.commission.*;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.enums.CommissionEntryStatus;
import company.aura.erp.enums.CommissionSourceType;
import company.aura.erp.enums.CommissionStatementStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping("/entries")
    public ResponseEntity<Page<CommissionEntryResponse>> listEntries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long collaboratorId,
            @RequestParam(required = false) Long statementId,
            @RequestParam(required = false) List<CommissionEntryStatus> status,
            @RequestParam(required = false) CommissionSourceType sourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<CommissionEntryResponse> page = commissionService.listEntries(
                search,
                collaboratorId,
                statementId,
                status,
                sourceType,
                startDate,
                endDate,
                userPrincipal.getCompanyId(),
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/entries/{entryId}")
    public ResponseEntity<CommissionEntryResponse> findEntryById(
            @PathVariable Long entryId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CommissionEntryResponse response = commissionService.findEntryById(entryId, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<Void> cancelEntry(
            @PathVariable Long entryId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        commissionService.cancelEntry(entryId, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/statements")
    public ResponseEntity<CommissionStatementResponse> createStatement(
            @RequestBody @Valid CommissionStatementCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CommissionStatementResponse response = commissionService.createStatement(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/statements")
    public ResponseEntity<Page<CommissionStatementSummaryResponse>> listStatements(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long collaboratorId,
            @RequestParam(required = false) List<CommissionStatementStatus> status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<CommissionStatementSummaryResponse> page = commissionService.listStatements(
                search,
                collaboratorId,
                status,
                startDate,
                endDate,
                userPrincipal.getCompanyId(),
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/statements/{statementId}")
    public ResponseEntity<CommissionStatementResponse> findStatementById(
            @PathVariable Long statementId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CommissionStatementResponse response = commissionService.findStatementById(statementId, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/statements/{statementId}/approve")
    public ResponseEntity<CommissionStatementResponse> approveStatement(
            @PathVariable Long statementId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CommissionStatementResponse response = commissionService.approveStatement(statementId, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/statements/{statementId}/pay")
    public ResponseEntity<CommissionStatementResponse> payStatement(
            @PathVariable Long statementId,
            @RequestBody @Valid PayableTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CommissionStatementResponse response = commissionService.payStatement(statementId, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/statements/{statementId}")
    public ResponseEntity<Void> cancelStatement(
            @PathVariable Long statementId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        commissionService.cancelStatement(statementId, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
