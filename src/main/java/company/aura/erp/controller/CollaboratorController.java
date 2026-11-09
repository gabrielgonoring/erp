package company.aura.erp.controller;

import company.aura.erp.dto.collaborator.*;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CollaboratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequestMapping("/api/collaborators")
@RequiredArgsConstructor
public class CollaboratorController {

    private final CollaboratorService collaboratorService;

    @PostMapping
    public ResponseEntity<CollaboratorResponse> create(
            @RequestBody @Valid CollaboratorCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("create() - START - companyId={}, name={}", userPrincipal.getCompanyId(), request != null ? request.getName() : null);

        CollaboratorResponse response = collaboratorService.create(request, userPrincipal.getCompanyId());

        log.info("create() = END - collaboratorId={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollaboratorResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid CollaboratorUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("update() - START - id={}, companyId={}", id, userPrincipal.getCompanyId());

        CollaboratorResponse response = collaboratorService.update(id, request, userPrincipal.getCompanyId());

        log.info("update() = END - id={}", response.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("delete() - START - id={}, companyId={}", id, userPrincipal.getCompanyId());

        collaboratorService.delete(id, userPrincipal.getCompanyId());

        log.info("delete() = END - id={}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<CollaboratorSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("list() - START - companyId={}, search={}, status={}", userPrincipal.getCompanyId(), search, status);

        var page = collaboratorService.listPaginated(search, status, userPrincipal.getCompanyId(), pageable);

        log.info("list() = END - totalElements={}", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollaboratorResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("findById() - START - id={}, companyId={}", id, userPrincipal.getCompanyId());

        CollaboratorResponse response = collaboratorService.findById(id, userPrincipal.getCompanyId());

        log.info("findById() = END - id={}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/commission-report")
    public ResponseEntity<CollaboratorCommissionReportResponse> getSoldItems(
            @PathVariable Long id,
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("getSoldItems() - START - id={}, companyId={}", id, userPrincipal.getCompanyId());

        var response = collaboratorService.getSoldItemsReport(id, startDate, endDate, userPrincipal.getCompanyId());

        log.info("getSoldItems() = END - id={}, totalItems={}", id, response.getItems().size());
        return ResponseEntity.ok(response);
    }


}
