package company.aura.erp.service;

import company.aura.erp.dto.catalog.CatalogItemResponse;
import company.aura.erp.dto.catalog.CatalogPhysicalItemCreateRequest;
import company.aura.erp.dto.company.CompanyResponse;
import company.aura.erp.dto.invoice.InvoiceCatalogItemsImportResponse;
import company.aura.erp.dto.invoice.InvoiceCreateOrderRequest;
import company.aura.erp.dto.invoice.InvoiceImportCarrierRequest;
import company.aura.erp.dto.invoice.InvoiceImportCatalogItemsRequest;
import company.aura.erp.dto.invoice.InvoiceImportCustomerRequest;
import company.aura.erp.dto.invoice.InvoiceSkippedCatalogItemResponse;
import company.aura.erp.dto.order.OrderCreateRequest;
import company.aura.erp.dto.order.OrderItemCreateRequest;
import company.aura.erp.dto.order.OrderResponse;
import company.aura.erp.dto.order.OrderShippingRequest;
import company.aura.erp.dto.partner.PartnerCreateRequest;
import company.aura.erp.dto.partner.PartnerResponse;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.mapper.CatalogItemMapper;
import company.aura.erp.mapper.OrderItemMapper;
import company.aura.erp.mapper.OrderMapper;
import company.aura.erp.mapper.OrderShippingMapper;
import company.aura.erp.mapper.PartnerMapper;
import company.aura.erp.modules.invoice.domain.InvoiceCarrierData;
import company.aura.erp.modules.invoice.domain.InvoiceItemData;
import company.aura.erp.modules.invoice.domain.InvoicePartnerData;
import company.aura.erp.modules.invoice.domain.ParsedInvoiceXml;
import company.aura.erp.modules.invoice.service.InvoiceXmlReaderService;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.InvoiceService.LoadedInvoiceXml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceXmlImportService {

    private final InvoiceService invoiceService;
    private final InvoiceXmlReaderService invoiceXmlReaderService;
    private final CatalogItemService catalogItemService;
    private final PartnerService partnerService;
    private final OrderService orderService;
    private final CompanyService companyService;

    @Transactional
    public InvoiceCatalogItemsImportResponse importCatalogItemsFromInvoice(InvoiceImportCatalogItemsRequest request, UserPrincipal currentUser) {
        Long invoiceId = request.getInvoiceId();
        Long companyId = currentUser.getCompanyId();

        log.info("importCatalogItemsFromInvoice() - START - invoiceId={}, companyId={}, categoryId={}", invoiceId, companyId, request.getCategoryId());

        LoadedInvoiceXml loaded = invoiceService.loadAndParseXml(invoiceId, companyId);
        List<InvoiceItemData> xmlItems = loaded.parsedXml().getItems();

        if (xmlItems == null || xmlItems.isEmpty()) {
            log.warn("importCatalogItemsFromInvoice() - WARN - No items found in XML for invoiceId={}", invoiceId);
            return InvoiceCatalogItemsImportResponse.builder()
                    .invoiceId(invoiceId)
                    .totalItemsInXml(0)
                    .build();
        }

        List<CatalogItemResponse> createdItems = new ArrayList<>();
        List<CatalogItemResponse> existingItems = new ArrayList<>();
        List<InvoiceSkippedCatalogItemResponse> skippedItems = new ArrayList<>();
        Map<String, CatalogItemResponse> resolvedInBatch = new HashMap<>();
        Set<Long> seenExistingIds = new HashSet<>();

        for (InvoiceItemData xmlItem : xmlItems) {
            processXmlCatalogItem(
                    xmlItem,
                    request.getCategoryId(),
                    companyId,
                    resolvedInBatch,
                    seenExistingIds,
                    createdItems,
                    existingItems,
                    skippedItems
            );
        }

        InvoiceCatalogItemsImportResponse response = InvoiceCatalogItemsImportResponse.builder()
                .invoiceId(invoiceId)
                .totalItemsInXml(xmlItems.size())
                .createdCount(createdItems.size())
                .existingCount(existingItems.size())
                .skippedCount(skippedItems.size())
                .createdItems(createdItems)
                .existingItems(existingItems)
                .skippedItems(skippedItems)
                .build();

        log.info("importCatalogItemsFromInvoice() - END - invoiceId={}, total={}, created={}, existing={}, skipped={}", invoiceId, response.getTotalItemsInXml(), response.getCreatedCount(), response.getExistingCount(), response.getSkippedCount());
        return response;
    }

    @Transactional
    public PartnerResponse createCustomerFromInvoice(InvoiceImportCustomerRequest request, UserPrincipal currentUser) {

        Long invoiceId = request.getInvoiceId();
        Long companyId = currentUser.getCompanyId();

        log.info("createCustomerFromInvoice() - START - invoiceId={}, companyId={}", invoiceId, companyId);

        LoadedInvoiceXml loaded = invoiceService.loadAndParseXml(invoiceId, companyId);
        InvoicePartnerData customerData = resolveCustomerCandidate(loaded.parsedXml(), currentUser);

        if (customerData == null || !PartnerMapper.hasValidDocument(customerData.getDocument()) || StringUtils.isBlank(customerData.getName())) {
            log.warn("createCustomerFromInvoice() - WARN - Customer in XML cannot be identified - invoiceId={}", invoiceId);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível identificar o cliente no XML da nota fiscal (CPF/CNPJ ou nome ausente/inválido)."
            );
        }

        String normalizedDoc = PartnerMapper.normalizeDocument(customerData.getDocument());
        if (partnerService.findByDocumentAndCompanyId(normalizedDoc, companyId).isPresent()) {
            log.warn("createCustomerFromInvoice() - WARN - Customer already exists for document={}, companyId={}", normalizedDoc, companyId);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um cliente/parceiro cadastrado com o documento: " + normalizedDoc
            );
        }

        PartnerCreateRequest partnerRequest = PartnerMapper.toCustomerCreateRequest(customerData);
        PartnerResponse created = partnerService.save(partnerRequest, companyId);

        log.info("createCustomerFromInvoice() - END - invoiceId={}, partnerId={}, document={}", invoiceId, created.id(), created.document());
        return created;
    }

    @Transactional
    public PartnerResponse createCarrierFromInvoice(InvoiceImportCarrierRequest request, UserPrincipal currentUser) {
        Long invoiceId = request.getInvoiceId();
        Long companyId = currentUser.getCompanyId();
        log.info("createCarrierFromInvoice() - START - invoiceId={}, companyId={}", invoiceId, companyId);

        LoadedInvoiceXml loaded = invoiceService.loadAndParseXml(invoiceId, companyId);
        ParsedInvoiceXml parsedXml = loaded.parsedXml();

        InvoiceCarrierData carrierData = parsedXml.getFreight() != null ? parsedXml.getFreight().getCarrier() : null;
        if (carrierData == null) {
            log.warn("createCarrierFromInvoice() - WARN - No carrier tag found in XML - invoiceId={}", invoiceId);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Nenhuma transportadora encontrada no XML da nota fiscal."
            );
        }

        if (!PartnerMapper.hasValidDocument(carrierData.getDocument()) || StringUtils.isBlank(carrierData.getName())) {
            log.warn("createCarrierFromInvoice() - WARN - Carrier in XML cannot be identified - invoiceId={}", invoiceId);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível identificar a transportadora no XML da nota fiscal (CPF/CNPJ ou nome ausente/inválido)."
            );
        }

        String normalizedDoc = PartnerMapper.normalizeDocument(carrierData.getDocument());
        if (partnerService.findByDocumentAndCompanyId(normalizedDoc, companyId).isPresent()) {
            log.warn("createCarrierFromInvoice() - WARN - Carrier already exists for document={}, companyId={}",
                    normalizedDoc, companyId);
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma transportadora/parceiro cadastrada com o documento: " + normalizedDoc
            );
        }

        PartnerCreateRequest carrierRequest = PartnerMapper.toCarrierCreateRequest(carrierData);
        PartnerResponse created = partnerService.save(carrierRequest, companyId);

        log.info("createCarrierFromInvoice() - END - invoiceId={}, carrierId={}, document={}",
                invoiceId, created.id(), created.document());
        return created;
    }

    @Transactional
    public OrderResponse createOrderFromInvoice(InvoiceCreateOrderRequest request, UserPrincipal currentUser) {
        Long invoiceId = request.getInvoiceId();
        Long companyId = currentUser.getCompanyId();
        log.info("createOrderFromInvoice() - START - invoiceId={}, companyId={}", invoiceId, companyId);

        LoadedInvoiceXml loaded = invoiceService.loadAndParseXml(invoiceId, companyId);
        ParsedInvoiceXml parsedXml = loaded.parsedXml();

        PartnerResponse partner = resolveExistingOrderPartner(loaded.invoice(), parsedXml, currentUser);
        List<OrderItemCreateRequest> orderItems = resolveExistingOrderItems(parsedXml.getItems(), companyId, invoiceId);
        Long carrierId = resolveExistingCarrierId(parsedXml, companyId);
        OrderShippingRequest shipping = OrderShippingMapper.toOrderShippingRequest(parsedXml, carrierId);
        String notes = resolveOrderNotes(request, parsedXml);

        OrderCreateRequest orderRequest = OrderMapper.toOrderCreateRequest(partner.id(), notes, orderItems, shipping);

        OrderResponse createdOrder = orderService.openOrder(orderRequest, companyId);

        log.info("createOrderFromInvoice() - END - invoiceId={}, orderId={}, partnerId={}, itemsCount={}", invoiceId, createdOrder.getId(), partner.id(), orderItems.size());
        return createdOrder;
    }

    private void processXmlCatalogItem(
            InvoiceItemData xmlItem,
            Long categoryId,
            Long companyId,
            Map<String, CatalogItemResponse> resolvedInBatch,
            Set<Long> seenExistingIds,
            List<CatalogItemResponse> createdItems,
            List<CatalogItemResponse> existingItems,
            List<InvoiceSkippedCatalogItemResponse> skippedItems
    ) {
        if (!CatalogItemMapper.hasIdentifyingCode(xmlItem)) {
            log.warn("processXmlCatalogItem() - WARN - Item cannot be identified (missing cProd and valid cEAN) - itemNumber={}", xmlItem != null ? xmlItem.getItemNumber() : null);
            skippedItems.add(buildSkippedItem(
                    xmlItem, "Item sem código do produto (cProd) e sem GTIN/EAN válido para identificar se já existe no catálogo."
            ));
            return;
        }

        Optional<CatalogItemResponse> existing = findExistingCatalogItem(xmlItem, companyId, resolvedInBatch);
        if (existing.isPresent()) {
            CatalogItemResponse found = existing.get();
            registerInBatchCache(xmlItem, found, resolvedInBatch);
            if (seenExistingIds.add(found.id())) {
                existingItems.add(found);
            }
            return;
        }

        Optional<String> validationError = CatalogItemMapper.validateCatalogCreatable(xmlItem);
        if (validationError.isPresent()) {
            log.warn("processXmlCatalogItem() - WARN - Item skipped due to invalid/missing required fields - itemNumber={}, reason={}", xmlItem.getItemNumber(), validationError.get());
            skippedItems.add(buildSkippedItem(xmlItem, validationError.get()));
            return;
        }

        CatalogPhysicalItemCreateRequest createRequest = CatalogItemMapper.toPhysicalCreateRequest(xmlItem, categoryId);
        CatalogItemResponse saved = catalogItemService.savePhysical(createRequest, companyId);
        createdItems.add(saved);
        seenExistingIds.add(saved.id());
        registerInBatchCache(xmlItem, saved, resolvedInBatch);
    }

    private Optional<CatalogItemResponse> findExistingCatalogItem(InvoiceItemData xmlItem, Long companyId, Map<String, CatalogItemResponse> resolvedInBatch) {
        if (CatalogItemMapper.isValidGtinEan(xmlItem.getGtinEan())) {
            CatalogItemResponse cached = resolvedInBatch.get("GTIN:" + CatalogItemMapper.normalizeGtinEan(xmlItem.getGtinEan()));
            if (cached != null) {
                return Optional.of(cached);
            }
        }
        if (StringUtils.isNotBlank(xmlItem.getProductCode())) {
            CatalogItemResponse cached = resolvedInBatch.get("CODE:" + xmlItem.getProductCode().trim().toUpperCase());
            if (cached != null) {
                return Optional.of(cached);
            }
        }
        return catalogItemService.findByXmlIdentifiers(companyId, xmlItem.getProductCode(), xmlItem.getGtinEan());
    }

    private void registerInBatchCache(InvoiceItemData xmlItem, CatalogItemResponse catalogItem, Map<String, CatalogItemResponse> resolvedInBatch) {
        if (CatalogItemMapper.isValidGtinEan(xmlItem.getGtinEan())) {
            resolvedInBatch.put("GTIN:" + CatalogItemMapper.normalizeGtinEan(xmlItem.getGtinEan()), catalogItem);
        }
        if (StringUtils.isNotBlank(xmlItem.getProductCode())) {
            resolvedInBatch.put("CODE:" + xmlItem.getProductCode().trim().toUpperCase(), catalogItem);
        }
    }

    private InvoiceSkippedCatalogItemResponse buildSkippedItem(InvoiceItemData xmlItem, String reason) {
        return InvoiceSkippedCatalogItemResponse.builder()
                .itemNumber(xmlItem != null ? xmlItem.getItemNumber() : null)
                .productCode(xmlItem != null ? xmlItem.getProductCode() : null)
                .gtinEan(xmlItem != null ? xmlItem.getGtinEan() : null)
                .description(xmlItem != null ? xmlItem.getDescription() : null)
                .reason(reason)
                .build();
    }

    private InvoicePartnerData resolveCustomerCandidate(ParsedInvoiceXml parsedXml, UserPrincipal currentUser) {
        if (parsedXml == null) {
            return null;
        }
        InvoicePartnerData recipient = parsedXml.getRecipient();
        if (recipient == null) {
            return null;
        }

        String ownCompanyCnpj = PartnerMapper.normalizeDocument(resolveOwnCompanyCnpj(currentUser));
        String recipientDoc = PartnerMapper.normalizeDocument(recipient.getDocument());
        if (StringUtils.isNotBlank(ownCompanyCnpj) && ownCompanyCnpj.equals(recipientDoc)) {
            log.warn("resolveCustomerCandidate() - WARN - Recipient matches logged company CNPJ={}", ownCompanyCnpj);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O destinatário do XML corresponde à própria empresa logada e não pode ser cadastrado como cliente."
            );
        }

        return recipient;
    }

    private PartnerResponse resolveExistingOrderPartner(InvoiceEntity invoice, ParsedInvoiceXml parsedXml, UserPrincipal currentUser) {
        Long companyId = currentUser.getCompanyId();
        String ownCompanyCnpj = resolveOwnCompanyCnpj(currentUser);
        InvoicePartnerData counterparty = invoiceXmlReaderService.resolveCounterpartyPartner(parsedXml, ownCompanyCnpj);

        if (counterparty != null && PartnerMapper.hasValidDocument(counterparty.getDocument())) {
            String normalizedDoc = PartnerMapper.normalizeDocument(counterparty.getDocument());
            return partnerService.findByDocumentAndCompanyId(normalizedDoc, companyId)
                    .orElseThrow(() -> {
                        log.warn("resolveExistingOrderPartner() - WARN - Partner not found for XML document={}, companyId={}", normalizedDoc, companyId);
                        return new ResponseStatusException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "Não foi possível criar o pedido: parceiro não encontrado para o documento " + normalizedDoc
                        );
                    });
        }

        if (invoice.getPartnerId() != null) {
            return partnerService.findActiveByIdAndCompanyId(invoice.getPartnerId(), companyId)
                    .orElseThrow(() -> {
                        log.warn("resolveExistingOrderPartner() - WARN - Invoice partnerId={} not found/inactive for companyId={}", invoice.getPartnerId(), companyId);
                        return new ResponseStatusException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "Não foi possível criar o pedido: parceiro vinculado à nota fiscal não encontrado ou inativo."
                        );
                    });
        }

        log.warn("resolveExistingOrderPartner() - WARN - Could not identify partner from XML - invoiceId={}", invoice.getId());
        throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Não foi possível criar o pedido: parceiro não identificado no XML da nota fiscal."
        );
    }

    private List<OrderItemCreateRequest> resolveExistingOrderItems(List<InvoiceItemData> xmlItems, Long companyId, Long invoiceId) {
        if (xmlItems == null || xmlItems.isEmpty()) {
            log.warn("resolveExistingOrderItems() - WARN - No items in XML for invoiceId={}", invoiceId);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível criar o pedido: a nota fiscal não possui itens no XML."
            );
        }

        List<OrderItemCreateRequest> orderItems = new ArrayList<>();
        List<String> missingItems = new ArrayList<>();

        for (InvoiceItemData xmlItem : xmlItems) {
            if (!CatalogItemMapper.hasIdentifyingCode(xmlItem)) {
                missingItems.add(formatItemLabel(xmlItem, "sem código cProd/GTIN para identificação"));
                continue;
            }

            if (xmlItem.getQuantity() == null || xmlItem.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                missingItems.add(formatItemLabel(xmlItem, "quantidade inválida"));
                continue;
            }

            Optional<CatalogItemResponse> existingCatalogItem = catalogItemService.findByXmlIdentifiers(
                    companyId, xmlItem.getProductCode(), xmlItem.getGtinEan()
            );

            if (existingCatalogItem.isEmpty()) {
                missingItems.add(formatItemLabel(xmlItem, "não encontrado no catálogo"));
                continue;
            }

            orderItems.add(OrderItemMapper.toOrderItemCreateRequest(xmlItem, existingCatalogItem.get()));
        }

        if (!missingItems.isEmpty()) {
            log.warn("resolveExistingOrderItems() - WARN - Order creation aborted for invoiceId={}, missingItems={}", invoiceId, missingItems);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível criar o pedido pois nem todos os itens da nota fiscal foram recuperados no catálogo: " + String.join("; ", missingItems)
            );
        }

        return orderItems;
    }

    private Long resolveExistingCarrierId(ParsedInvoiceXml parsedXml, Long companyId) {
        if (parsedXml == null || parsedXml.getFreight() == null || parsedXml.getFreight().getCarrier() == null) {
            return null;
        }
        String carrierDoc = parsedXml.getFreight().getCarrier().getDocument();
        if (!PartnerMapper.hasValidDocument(carrierDoc)) {
            return null;
        }
        return partnerService.findByDocumentAndCompanyId(carrierDoc, companyId)
                .map(PartnerResponse::id)
                .orElse(null);
    }

    private String resolveOwnCompanyCnpj(UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getCompanyId() == null) {
            return null;
        }
        try {
            CompanyResponse company = companyService.findById(currentUser);
            return company != null ? company.cnpj() : null;
        } catch (ResponseStatusException ex) {
            log.warn("resolveOwnCompanyCnpj() - WARN - Could not load company CNPJ for companyId={}, error={}",
                    currentUser.getCompanyId(), ex.getReason());
            return null;
        }
    }

    private String resolveOrderNotes(InvoiceCreateOrderRequest request, ParsedInvoiceXml parsedXml) {
        if (StringUtils.isNotBlank(request.getNotes())) {
            return request.getNotes().trim();
        }
        if (parsedXml != null && parsedXml.getIdentification() != null
                && parsedXml.getIdentification().getInvoiceNumber() != null) {
            return "Pedido gerado a partir da nota fiscal nº " + parsedXml.getIdentification().getInvoiceNumber();
        }
        return null;
    }

    private String formatItemLabel(InvoiceItemData xmlItem, String detail) {
        String code = StringUtils.isNotBlank(xmlItem.getProductCode()) ? xmlItem.getProductCode() : xmlItem.getGtinEan();
        String desc = StringUtils.isNotBlank(xmlItem.getDescription()) ? xmlItem.getDescription() : "sem descrição";
        return String.format("Item %s (%s - %s): %s",
                xmlItem.getItemNumber() != null ? xmlItem.getItemNumber() : "?",
                StringUtils.isNotBlank(code) ? code : "sem código",
                desc,
                detail);
    }
}
