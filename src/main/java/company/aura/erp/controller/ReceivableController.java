package company.aura.erp.controller;

import company.aura.erp.dto.order.*;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.ReceivedService;
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
@RequestMapping("/api/receivables")
@AllArgsConstructor
public class ReceivableController {

    private ReceivedService receivedService;

    @PostMapping
    public ResponseEntity<ReceivableResponse> createReceivable(
            @RequestBody @Valid ReceivableRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ReceivableResponse response = receivedService.processSingleReceivable(userPrincipal.getCompanyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{receivableId}")
    public ResponseEntity<ReceivableResponse> updateReceivable(
            @PathVariable Long receivableId,
            @RequestBody @Valid ReceivableUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ReceivableResponse response = receivedService.updateReceivable(userPrincipal.getCompanyId(), receivableId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{receivableId}/transactions/{transactionId}")
    public ResponseEntity<ReceivableResponse> updatePaymentTransaction(
            @PathVariable Long receivableId,
            @PathVariable Long transactionId,
            @RequestBody @Valid PaymentTransactionUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ReceivableResponse response = receivedService.updatePaymentTransaction(
                userPrincipal.getCompanyId(),
                receivableId,
                transactionId,
                request
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{receivableId}/pay")
    public ResponseEntity<ReceivableResponse> payReceivable(
            @PathVariable Long receivableId,
            @RequestBody @Valid PaymentTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ReceivableResponse response = receivedService.payReceivable(userPrincipal.getCompanyId(), receivableId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{receivableId}")
    public ResponseEntity<Void> cancelReceivable(
            @PathVariable Long receivableId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        receivedService.cancelReceivable(userPrincipal.getCompanyId(), receivableId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{receivableId}/payment-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceivableResponse> uploadPaymentFile(
            @PathVariable Long receivableId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "bankSlipCode", required = false) String bankSlipCode,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ReceivableResponse response = receivedService.uploadPaymentFile( userPrincipal.getCompanyId(), receivableId, file, bankSlipCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{receivableId}/payment-file")
    public ResponseEntity<PaymentFileUrlResponse> getPaymentFile(
            @PathVariable Long receivableId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String temporaryUrl = receivedService.getPaymentFileUrl(userPrincipal.getCompanyId(), receivableId);
        return ResponseEntity.ok(new PaymentFileUrlResponse(temporaryUrl));
    }

    @DeleteMapping("/{receivableId}/payment-file")
    public ResponseEntity<Void> deletePaymentFile(
            @PathVariable Long receivableId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        receivedService.deletePaymentFile(userPrincipal.getCompanyId(), receivableId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping
    public ResponseEntity<Page<ReceivableSummaryResponse>> listReceivables(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<ReceivableStatus> status,
            @RequestParam(required = false) List<PaymentMethod> paymentMethods,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<ReceivableSummaryResponse> page = receivedService.listReceivables(
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

    @GetMapping("/{receivableId}")
    public ResponseEntity<ReceivableResponse> findById(
            @PathVariable Long receivableId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ReceivableResponse response = receivedService.findById(userPrincipal.getCompanyId(), receivableId);
        return ResponseEntity.ok(response);
    }

}
