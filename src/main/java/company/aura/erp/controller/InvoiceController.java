package company.aura.erp.controller;

import company.aura.erp.dto.invoice.InvoiceCatalogItemsImportResponse;
import company.aura.erp.dto.invoice.InvoiceCreateOrderRequest;
import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceImportCarrierRequest;
import company.aura.erp.dto.invoice.InvoiceImportCatalogItemsRequest;
import company.aura.erp.dto.invoice.InvoiceImportCustomerRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.invoice.InvoiceUrlResponse;
import company.aura.erp.dto.order.OrderResponse;
import company.aura.erp.dto.partner.PartnerResponse;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.InvoiceService;
import company.aura.erp.service.InvoiceXmlImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceXmlImportService invoiceXmlImportService;

    @PostMapping(value = "/external", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceResponse> createInvoice(
            @ModelAttribute @Valid InvoiceCreateRequest formRequest,
            @RequestPart(value = "xmlFile", required = false) MultipartFile xmlFile,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("createInvoice() - START - partnerId={}, companyId={}",
                formRequest != null ? formRequest.getPartnerId() : null,
                userPrincipal != null ? userPrincipal.getCompanyId() : null);

        InvoiceResponse response = invoiceService.createExternalInvoice(formRequest, xmlFile, pdfFile, userPrincipal);

        log.info("createInvoice() - END - invoiceId={}", response != null ? response.getId() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/xml/catalog-items")
    public ResponseEntity<InvoiceCatalogItemsImportResponse> importCatalogItemsFromXml(
            @RequestBody @Valid InvoiceImportCatalogItemsRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("importCatalogItemsFromXml() - START - invoiceId={}, categoryId={}, companyId={}",
                request.getInvoiceId(), request.getCategoryId(), userPrincipal.getCompanyId());

        InvoiceCatalogItemsImportResponse response = invoiceXmlImportService.importCatalogItemsFromInvoice(request, userPrincipal);

        log.info("importCatalogItemsFromXml() - END - invoiceId={}, created={}, existing={}, skipped={}",
                request.getInvoiceId(), response.getCreatedCount(), response.getExistingCount(), response.getSkippedCount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/xml/customer")
    public ResponseEntity<PartnerResponse> createCustomerFromXml(
            @RequestBody @Valid InvoiceImportCustomerRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("createCustomerFromXml() - START - invoiceId={}, companyId={}",
                request.getInvoiceId(), userPrincipal.getCompanyId());

        PartnerResponse response = invoiceXmlImportService.createCustomerFromInvoice(request, userPrincipal);

        log.info("createCustomerFromXml() - END - invoiceId={}, partnerId={}",
                request.getInvoiceId(), response != null ? response.id() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/xml/carrier")
    public ResponseEntity<PartnerResponse> createCarrierFromXml(
            @RequestBody @Valid InvoiceImportCarrierRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("createCarrierFromXml() - START - invoiceId={}, companyId={}",
                request.getInvoiceId(), userPrincipal.getCompanyId());

        PartnerResponse response = invoiceXmlImportService.createCarrierFromInvoice(request, userPrincipal);

        log.info("createCarrierFromXml() - END - invoiceId={}, carrierId={}",
                request.getInvoiceId(), response != null ? response.id() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/xml/order")
    public ResponseEntity<OrderResponse> createOrderFromXml(
            @RequestBody @Valid InvoiceCreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("createOrderFromXml() - START - invoiceId={}, companyId={}",
                request.getInvoiceId(), userPrincipal.getCompanyId());

        OrderResponse response = invoiceXmlImportService.createOrderFromInvoice(request, userPrincipal);

        log.info("createOrderFromXml() - END - invoiceId={}, orderId={}",
                request.getInvoiceId(), response != null ? response.getId() : null);
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
