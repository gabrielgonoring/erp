package company.aura.erp.controller;

import company.aura.erp.dto.serviceorder.*;
import company.aura.erp.enums.ServiceOrderStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.ServiceOrderService;
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
@RequestMapping("/api/service-orders")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    @GetMapping
    public ResponseEntity<Page<ServiceOrderSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<ServiceOrderStatus> status,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) String objectIdentifier,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<ServiceOrderSummaryResponse> page = serviceOrderService.listSummarized(
                search,
                startDate,
                endDate,
                status,
                partnerId,
                technicianId,
                objectIdentifier,
                userPrincipal.getCompanyId(),
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrderResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ServiceOrderResponse response = serviceOrderService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ServiceOrderResponse> create(
            @RequestBody @Valid ServiceOrderCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ServiceOrderResponse response = serviceOrderService.create(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOrderResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid ServiceOrderUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ServiceOrderResponse response = serviceOrderService.update(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ServiceOrderItemResponse> addItem(
            @PathVariable Long id,
            @RequestBody @Valid ServiceOrderItemCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ServiceOrderItemResponse response = serviceOrderService.addItem(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<ServiceOrderItemResponse> updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody @Valid ServiceOrderItemUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ServiceOrderItemResponse response = serviceOrderService.updateItem(id, itemId, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        serviceOrderService.removeItem(id, itemId, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ServiceOrderResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ServiceOrderResponse response = serviceOrderService.cancel(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ServiceOrderResponse> complete(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid ServiceOrderCompleteRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ServiceOrderResponse response = serviceOrderService.complete(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }
}
