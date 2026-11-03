package company.aura.erp.controller;

import company.aura.erp.dto.order.*;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> openOrder(
            @RequestBody @Valid OrderCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderResponse response = orderService.openOrder(request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderItemResponse> addItem(
            @PathVariable Long id,
            @RequestBody @Valid OrderItemCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderItemResponse response = orderService.addItem(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderItemResponse> updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody @Valid OrderItemUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderItemResponse response = orderService.updateItem(id, itemId, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        orderService.removeItem(id, itemId, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<OrderResponse> closeOrder(
            @PathVariable Long id,
            @RequestBody @Valid OrderCloseRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderResponse response = orderService.closeOrder(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

}
