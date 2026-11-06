package company.aura.erp.controller;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping(value = "/external", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceResponse> createInvoice(
            @ModelAttribute @Valid InvoiceCreateRequest formRequest,
            @RequestPart(value = "xmlFile", required = false) MultipartFile xmlFile,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {

        InvoiceResponse response = invoiceService.createExternalInvoice(formRequest, xmlFile, pdfFile, userPrincipal);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        InvoiceResponse response = invoiceService.findById(id, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> list(
            @RequestParam(name = "partnerId", required = false) List<Long> partnerId,
            @RequestParam(name = "partnerIds", required = false) List<Long> partnerIds,
            @RequestParam(required = false) String externalReference,
            @RequestParam(required = false) Integer operationType,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<Long> effectivePartnerIds = partnerIds != null && !partnerIds.isEmpty() ? partnerIds : partnerId;
        List<InvoiceResponse> responses;
        if ((effectivePartnerIds == null || effectivePartnerIds.isEmpty())
                && (externalReference == null || externalReference.isBlank())
                && operationType == null
                && documentType == null
                && (status == null || status.isBlank())) {
            responses = invoiceService.list(userPrincipal.getCompanyId());
        } else {
            responses = invoiceService.list(
                    userPrincipal.getCompanyId(),
                    effectivePartnerIds,
                    externalReference,
                    operationType,
                    documentType,
                    status
            );
        }
        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<List<InvoiceResponse>> list(UserPrincipal userPrincipal) {
        return list(null, null, null, null, null, null, userPrincipal);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Void> getPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String temporaryUrl = invoiceService.getPdfUrl(id, userPrincipal);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(temporaryUrl))
                .build();
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<Void> getXml(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String temporaryUrl = invoiceService.getXmlUrl(id, userPrincipal);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(temporaryUrl))
                .build();
    }

}
