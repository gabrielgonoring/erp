package company.aura.erp.controller;

import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.order.*;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

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

    @PutMapping("/{id}/items/{itemId}")
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

    @PostMapping(value = "/{orderId}/invoice/{invoiceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrderInvoiceResponse> linkInvoice(
            @PathVariable Long orderId,
            @PathVariable Long invoiceId,
            @ModelAttribute @Valid LinkOrderInvoice request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        OrderInvoiceResponse response = orderService.linkInvoice(
                orderId,
                invoiceId,
                request,
                userPrincipal
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{orderId}/invoice/{invoiceId}")
    public ResponseEntity<Void> unlinkInvoice(
            @PathVariable Long orderId,
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        orderService.unlinkInvoice(orderId, invoiceId, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{orderId}/invoices")
    public ResponseEntity<List<OrderInvoiceResponse>> getOrderInvoices(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<OrderInvoiceResponse> response = orderService.findInvoicesByOrderId(orderId, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<OrderStatus> status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<OrderSummaryResponse> page = orderService.listSummarized(
                search,
                startDate,
                endDate,
                status,
                userPrincipal.getCompanyId(),
                pageable
        );
        return ResponseEntity.ok(page);
    }

}
