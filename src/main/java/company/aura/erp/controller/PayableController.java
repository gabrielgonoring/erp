package company.aura.erp.controller;

import company.aura.erp.dto.order.PaymentFileUrlResponse;
import company.aura.erp.dto.payable.PayableRequest;
import company.aura.erp.dto.payable.PayableResponse;
import company.aura.erp.dto.payable.PayableSummaryResponse;
import company.aura.erp.dto.payable.PayableTransactionRequest;
import company.aura.erp.enums.PayableStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.PayableService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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
@RequestMapping("/api/payables")
@AllArgsConstructor
public class PayableController {

    private PayableService payableService;

    @PostMapping
    public ResponseEntity<PayableResponse> createPayable(
            @RequestBody @Valid PayableRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        PayableResponse response = payableService.createPayable(userPrincipal.getCompanyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{payableId}/pay")
    public ResponseEntity<PayableResponse> payPayable(
            @PathVariable Long payableId,
            @RequestBody @Valid PayableTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        PayableResponse response = payableService.payPayable(userPrincipal.getCompanyId(), payableId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{payableId}")
    public ResponseEntity<Void> cancelPayable(
            @PathVariable Long payableId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        payableService.cancelPayable(userPrincipal.getCompanyId(), payableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{payableId}/payment-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PayableResponse> uploadPaymentFile(
            @PathVariable Long payableId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "transactionId", required = false) Long transactionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        PayableResponse response = payableService.uploadPaymentFile(userPrincipal.getCompanyId(), payableId, file, transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{payableId}/payment-file")
    public ResponseEntity<PaymentFileUrlResponse> getPaymentFile(
            @PathVariable Long payableId,
            @RequestParam(value = "transactionId", required = false) Long transactionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String temporaryUrl = payableService.getPaymentFileUrl(userPrincipal.getCompanyId(), payableId, transactionId);
        return ResponseEntity.ok(new PaymentFileUrlResponse(temporaryUrl));
    }

    @DeleteMapping("/{payableId}/payment-file")
    public ResponseEntity<Void> deletePaymentFile(
            @PathVariable Long payableId,
            @RequestParam(value = "transactionId", required = false) Long transactionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        payableService.deletePaymentFile(userPrincipal.getCompanyId(), payableId, transactionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<PayableSummaryResponse>> listPayables(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<PayableStatus> status,
            @RequestParam(required = false) List<PaymentMethod> paymentMethods,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<PayableSummaryResponse> page = payableService.listPayables(
                search,
                startDate,
                endDate,
                status,
                paymentMethods,
                userPrincipal.getCompanyId(),
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{payableId}")
    public ResponseEntity<PayableResponse> findById(
            @PathVariable Long payableId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        PayableResponse response = payableService.findById(userPrincipal.getCompanyId(), payableId);
        return ResponseEntity.ok(response);
    }
}
