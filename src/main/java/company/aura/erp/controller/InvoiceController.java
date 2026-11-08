package company.aura.erp.controller;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.invoice.InvoiceUrlResponse;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.InvoiceService;
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
    public ResponseEntity<Page<InvoiceResponse>> list(
            @RequestParam(required = false) List<Long> partnerIds,
            @RequestParam(required = false) String externalReference,
            @RequestParam(required = false) Integer operationType,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Page<InvoiceResponse> response = invoiceService.list(
                userPrincipal.getCompanyId(),
                partnerIds,
                externalReference,
                operationType,
                documentType,
                status,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<InvoiceUrlResponse> getPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String temporaryUrl = invoiceService.getPdfUrl(id, userPrincipal);
        return ResponseEntity.ok(new InvoiceUrlResponse(temporaryUrl));
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<InvoiceUrlResponse> getXml(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String temporaryUrl = invoiceService.getXmlUrl(id, userPrincipal);
        return ResponseEntity.ok(new InvoiceUrlResponse(temporaryUrl));
    }

}
