package company.aura.erp.controller;

import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.order.*;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderResponse response = orderService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable Long id,
            @RequestBody @Valid OrderUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderResponse response = orderService.updateOrder(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/shipping")
    public ResponseEntity<OrderShippingResponse> updateShipping(
            @PathVariable Long id,
            @RequestBody @Valid OrderShippingRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderShippingResponse response = orderService.updateShipping(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/shipping")
    public ResponseEntity<Void> deleteShipping(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        orderService.deleteShipping(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
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

    @PostMapping("/{id}/installments")
    public ResponseEntity<OrderInstallmentResponse> addInstallment(
            @PathVariable Long id,
            @RequestBody @Valid OrderInstallmentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderInstallmentResponse response = orderService.addInstallment(id, request, userPrincipal.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/installments/{installmentId}/payments")
    public ResponseEntity<OrderInstallmentResponse> payInstallment(
            @PathVariable Long id,
            @PathVariable Long installmentId,
            @RequestBody @Valid PaymentTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderInstallmentResponse response = orderService.payInstallment(id, installmentId, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/installments/{installmentId}")
    public ResponseEntity<OrderInstallmentResponse> updateInstallment(
            @PathVariable Long id,
            @PathVariable Long installmentId,
            @RequestBody @Valid PaymentTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderInstallmentResponse response = orderService.payInstallment(id, installmentId, request, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/installments/{installmentId}")
    public ResponseEntity<Void> cancelInstallment(
            @PathVariable Long id,
            @PathVariable Long installmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        orderService.cancelInstallment(id, installmentId, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/installments/{installmentId}/cancel")
    public ResponseEntity<OrderInstallmentResponse> cancelInstallmentPatch(
            @PathVariable Long id,
            @PathVariable Long installmentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderInstallmentResponse response = orderService.cancelInstallment(id, installmentId, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
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

    @PostMapping(value = "/{orderId}/invoice/external", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceResponse> linkExternalInvoice(
            @PathVariable Long orderId,
            @RequestPart(value = "xmlFile", required = false) MultipartFile xmlFile,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @ModelAttribute OrderExternalInvoiceRequest formRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        InvoiceResponse response = orderService.linkExternalInvoice(
                orderId,
                formRequest,
                xmlFile,
                pdfFile,
                userPrincipal
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<OrderSummaryResponse> page = orderService.listSummarized(search, userPrincipal.getCompanyId(), pageable);
        return ResponseEntity.ok(page);
    }

}
