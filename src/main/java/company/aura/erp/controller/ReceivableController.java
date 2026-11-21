package company.aura.erp.controller;

import company.aura.erp.dto.order.PaymentFileUrlResponse;
import company.aura.erp.dto.order.PaymentTransactionRequest;
import company.aura.erp.dto.order.ReceivableRequest;
import company.aura.erp.dto.order.ReceivableResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.ReceivedService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


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
        receivedService.cancelReceivable(receivableId, userPrincipal.getCompanyId());
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

}
