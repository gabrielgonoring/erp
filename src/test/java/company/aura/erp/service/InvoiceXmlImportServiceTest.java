package company.aura.erp.service;

import company.aura.erp.dto.catalog.CatalogItemResponse;
import company.aura.erp.dto.catalog.CatalogPhysicalItemCreateRequest;
import company.aura.erp.dto.catalog.CatalogPhysicalItemResponse;
import company.aura.erp.dto.company.CompanyResponse;
import company.aura.erp.dto.invoice.InvoiceCatalogItemsImportResponse;
import company.aura.erp.dto.invoice.InvoiceCreateOrderRequest;
import company.aura.erp.dto.invoice.InvoiceImportCarrierRequest;
import company.aura.erp.dto.invoice.InvoiceImportCatalogItemsRequest;
import company.aura.erp.dto.invoice.InvoiceImportCustomerRequest;
import company.aura.erp.dto.order.OrderCreateRequest;
import company.aura.erp.dto.order.OrderResponse;
import company.aura.erp.dto.partner.PartnerCreateRequest;
import company.aura.erp.dto.partner.PartnerResponse;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PersonType;
import company.aura.erp.modules.invoice.domain.InvoiceAddressData;
import company.aura.erp.modules.invoice.domain.InvoiceCarrierData;
import company.aura.erp.modules.invoice.domain.InvoiceFreightData;
import company.aura.erp.modules.invoice.domain.InvoiceIdentificationData;
import company.aura.erp.modules.invoice.domain.InvoiceItemData;
import company.aura.erp.modules.invoice.domain.InvoiceItemTaxData;
import company.aura.erp.modules.invoice.domain.InvoicePartnerData;
import company.aura.erp.modules.invoice.domain.InvoiceTotalsData;
import company.aura.erp.modules.invoice.domain.InvoiceVolumeData;
import company.aura.erp.modules.invoice.domain.ParsedInvoiceXml;
import company.aura.erp.modules.invoice.enumerate.FreightMode;
import company.aura.erp.modules.invoice.service.InvoiceXmlReaderService;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.InvoiceService.LoadedInvoiceXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceXmlImportServiceTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceXmlReaderService invoiceXmlReaderService;

    @Mock
    private CatalogItemService catalogItemService;

    @Mock
    private PartnerService partnerService;

    @Mock
    private OrderService orderService;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private InvoiceXmlImportService importService;

    private Long companyId;
    private Long invoiceId;
    private UserPrincipal userPrincipal;
    private InvoiceEntity invoiceEntity;
    private CompanyResponse companyResponse;

    @BeforeEach
    void setUp() {
        companyId = 10L;
        invoiceId = 100L;

        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(companyId)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();

        invoiceEntity = InvoiceEntity.builder()
                .id(invoiceId)
                .companyId(companyId)
                .partnerId(50L)
                .xmlUrl("https://r2.aura.company/xml/nfe.xml")
                .build();

        companyResponse = new CompanyResponse(
                companyId, "Minha Empresa Ltda", "Minha Empresa", "11111111000111",
                "123", null, 1,
                "Rua 1", "10", null, "Centro", "3550308", "São Paulo", "SP", "01001000",
                "1058", "Brasil"
        );
    }

    @Test
    @DisplayName("Deve criar itens identificáveis inexistentes, pular existentes e não criar não identificáveis")
    void importCatalogItemsSuccess() {
        InvoiceItemData newItem = InvoiceItemData.builder()
                .itemNumber(1)
                .productCode("SKU-NEW")
                .gtinEan("7891234567890")
                .description("Teclado Mecânico")
                .ncm("84716052")
                .cfop("5102")
                .unitOfMeasure("UN")
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("250.00"))
                .totalPrice(new BigDecimal("500.00"))
                .taxes(InvoiceItemTaxData.builder()
                        .origin(0)
                        .icmsCstOrCsosn("102")
                        .icmsRate(new BigDecimal("18.00"))
                        .build())
                .build();

        InvoiceItemData existingItem = InvoiceItemData.builder()
                .itemNumber(2)
                .productCode("SKU-EXIST")
                .gtinEan("SEM GTIN")
                .description("Mouse Óptico")
                .ncm("84716053")
                .cfop("5102")
                .unitOfMeasure("UN")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("80.00"))
                .build();

        InvoiceItemData unidentifiableItem = InvoiceItemData.builder()
                .itemNumber(3)
                .productCode(null)
                .gtinEan("SEM GTIN")
                .description("Item Sem Código")
                .ncm("84716053")
                .cfop("5102")
                .unitOfMeasure("UN")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("20.00"))
                .build();

        InvoiceItemData duplicateInXml = InvoiceItemData.builder()
                .itemNumber(4)
                .productCode("SKU-NEW")
                .gtinEan("7891234567890")
                .description("Teclado Mecânico")
                .ncm("84716052")
                .cfop("5102")
                .unitOfMeasure("UN")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("250.00"))
                .build();

        ParsedInvoiceXml parsedXml = ParsedInvoiceXml.builder()
                .items(List.of(newItem, existingItem, unidentifiableItem, duplicateInXml))
                .build();

        CatalogItemResponse savedNew = buildCatalogItemResponse(1001L, "SKU-NEW", "Teclado Mecânico", "250.00");
        CatalogItemResponse foundExisting = buildCatalogItemResponse(1002L, "SKU-EXIST", "Mouse Óptico", "80.00");

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, parsedXml));
        when(catalogItemService.findByXmlIdentifiers(companyId, "SKU-NEW", "7891234567890"))
                .thenReturn(Optional.empty());
        when(catalogItemService.findByXmlIdentifiers(companyId, "SKU-EXIST", "SEM GTIN"))
                .thenReturn(Optional.of(foundExisting));
        when(catalogItemService.savePhysical(any(CatalogPhysicalItemCreateRequest.class), eq(companyId)))
                .thenReturn(savedNew);

        InvoiceImportCatalogItemsRequest req = InvoiceImportCatalogItemsRequest.builder()
                .invoiceId(invoiceId)
                .categoryId(5L)
                .build();

        InvoiceCatalogItemsImportResponse response = importService.importCatalogItemsFromInvoice(req, userPrincipal);

        assertEquals(invoiceId, response.getInvoiceId());
        assertEquals(4, response.getTotalItemsInXml());
        assertEquals(1, response.getCreatedCount());
        assertEquals(1, response.getExistingCount());
        assertEquals(1, response.getSkippedCount());
        assertEquals(1001L, response.getCreatedItems().get(0).id());
        assertEquals(1002L, response.getExistingItems().get(0).id());
        assertEquals(3, response.getSkippedItems().get(0).getItemNumber());

        verify(catalogItemService, times(1)).savePhysical(any(CatalogPhysicalItemCreateRequest.class), eq(companyId));
    }

    @Test
    @DisplayName("Deve criar cliente a partir do XML quando identificável e ainda não existente")
    void createCustomerFromInvoiceSuccess() {
        InvoicePartnerData recipient = InvoicePartnerData.builder()
                .personType(PersonType.PJ)
                .document("22222222000122")
                .name("Cliente Destinatário Ltda")
                .indicatorIe(1)
                .stateRegistration("11223344")
                .email("contato@cliente.com")
                .phone("11999990000")
                .address(InvoiceAddressData.builder()
                        .street("Av Paulista")
                        .number("1000")
                        .neighborhood("Bela Vista")
                        .ibgeCityCode("3550308")
                        .city("São Paulo")
                        .state("SP")
                        .zipCode("01310100")
                        .countryCode("1058")
                        .countryName("Brasil")
                        .build())
                .build();

        ParsedInvoiceXml parsedXml = ParsedInvoiceXml.builder()
                .recipient(recipient)
                .build();

        PartnerResponse savedCustomer = buildPartnerResponse(200L, true, false, false, "22222222000122", "Cliente Destinatário Ltda");

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, parsedXml));
        when(companyService.findById(userPrincipal)).thenReturn(companyResponse);
        when(partnerService.findByDocumentAndCompanyId("22222222000122", companyId))
                .thenReturn(Optional.empty());
        when(partnerService.save(any(PartnerCreateRequest.class), eq(companyId)))
                .thenReturn(savedCustomer);

        PartnerResponse result = importService.createCustomerFromInvoice(
                InvoiceImportCustomerRequest.builder().invoiceId(invoiceId).build(),
                userPrincipal
        );

        assertNotNull(result);
        assertEquals(200L, result.id());
        assertTrue(result.isCustomer());

        ArgumentCaptor<PartnerCreateRequest> captor = ArgumentCaptor.forClass(PartnerCreateRequest.class);
        verify(partnerService).save(captor.capture(), eq(companyId));
        assertTrue(captor.getValue().isCustomer());
        assertFalse(captor.getValue().isSupplier());
        assertFalse(captor.getValue().isCarrier());
        assertEquals("22222222000122", captor.getValue().document());
    }

    @Test
    @DisplayName("Não deve criar cliente quando já existir parceiro com o mesmo documento")
    void createCustomerConflictWhenAlreadyExists() {
        InvoicePartnerData recipient = InvoicePartnerData.builder()
                .personType(PersonType.PJ)
                .document("22222222000122")
                .name("Cliente Destinatário Ltda")
                .build();

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, ParsedInvoiceXml.builder().recipient(recipient).build()));
        when(companyService.findById(userPrincipal)).thenReturn(companyResponse);
        when(partnerService.findByDocumentAndCompanyId("22222222000122", companyId))
                .thenReturn(Optional.of(buildPartnerResponse(200L, true, false, false, "22222222000122", "Cliente Existente")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                importService.createCustomerFromInvoice(
                        InvoiceImportCustomerRequest.builder().invoiceId(invoiceId).build(),
                        userPrincipal
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(partnerService, never()).save(any(), anyLong());
    }

    @Test
    @DisplayName("Não deve criar cliente quando não for possível identificar por ausência de CPF/CNPJ")
    void createCustomerUnidentifiableWithoutDocument() {
        InvoicePartnerData recipient = InvoicePartnerData.builder()
                .name("Consumidor Não Identificado")
                .document(null)
                .build();

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, ParsedInvoiceXml.builder().recipient(recipient).build()));
        when(companyService.findById(userPrincipal)).thenReturn(companyResponse);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                importService.createCustomerFromInvoice(
                        InvoiceImportCustomerRequest.builder().invoiceId(invoiceId).build(),
                        userPrincipal
                )
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(partnerService, never()).save(any(), anyLong());
    }

    @Test
    @DisplayName("Deve criar transportadora a partir do XML quando identificável e ainda não existente")
    void createCarrierFromInvoiceSuccess() {
        InvoiceCarrierData carrier = InvoiceCarrierData.builder()
                .personType(PersonType.PJ)
                .document("33333333000133")
                .name("Transportadora Rápida Ltda")
                .stateRegistration("99887766")
                .address("Rodovia Anhanguera Km 10")
                .city("Jundiaí")
                .state("SP")
                .build();

        ParsedInvoiceXml parsedXml = ParsedInvoiceXml.builder()
                .freight(InvoiceFreightData.builder()
                        .freightModeCode(0)
                        .freightMode(FreightMode.SENDER)
                        .carrier(carrier)
                        .build())
                .build();

        PartnerResponse savedCarrier = buildPartnerResponse(300L, false, false, true, "33333333000133", "Transportadora Rápida Ltda");

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, parsedXml));
        when(partnerService.findByDocumentAndCompanyId("33333333000133", companyId))
                .thenReturn(Optional.empty());
        when(partnerService.save(any(PartnerCreateRequest.class), eq(companyId)))
                .thenReturn(savedCarrier);

        PartnerResponse result = importService.createCarrierFromInvoice(
                InvoiceImportCarrierRequest.builder().invoiceId(invoiceId).build(),
                userPrincipal
        );

        assertNotNull(result);
        assertEquals(300L, result.id());
        assertTrue(result.isCarrier());

        ArgumentCaptor<PartnerCreateRequest> captor = ArgumentCaptor.forClass(PartnerCreateRequest.class);
        verify(partnerService).save(captor.capture(), eq(companyId));
        assertFalse(captor.getValue().isCustomer());
        assertFalse(captor.getValue().isSupplier());
        assertTrue(captor.getValue().isCarrier());
        assertEquals("33333333000133", captor.getValue().document());
    }

    @Test
    @DisplayName("Não deve criar transportadora quando documento estiver ausente ou já cadastrada")
    void createCarrierErrorsWhenUnidentifiableOrExisting() {
        InvoiceCarrierData carrierWithoutDoc = InvoiceCarrierData.builder()
                .name("Frete Próprio")
                .document(null)
                .build();

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, ParsedInvoiceXml.builder()
                        .freight(InvoiceFreightData.builder().carrier(carrierWithoutDoc).build())
                        .build()));

        ResponseStatusException unidentifiableEx = assertThrows(ResponseStatusException.class, () ->
                importService.createCarrierFromInvoice(
                        InvoiceImportCarrierRequest.builder().invoiceId(invoiceId).build(),
                        userPrincipal
                )
        );
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, unidentifiableEx.getStatusCode());

        InvoiceCarrierData existingCarrier = InvoiceCarrierData.builder()
                .name("Transportadora Existente")
                .document("33333333000133")
                .build();
        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, ParsedInvoiceXml.builder()
                        .freight(InvoiceFreightData.builder().carrier(existingCarrier).build())
                        .build()));
        when(partnerService.findByDocumentAndCompanyId("33333333000133", companyId))
                .thenReturn(Optional.of(buildPartnerResponse(300L, false, false, true, "33333333000133", "Transportadora Existente")));

        ResponseStatusException conflictEx = assertThrows(ResponseStatusException.class, () ->
                importService.createCarrierFromInvoice(
                        InvoiceImportCarrierRequest.builder().invoiceId(invoiceId).build(),
                        userPrincipal
                )
        );
        assertEquals(HttpStatus.CONFLICT, conflictEx.getStatusCode());
        verify(partnerService, never()).save(any(), anyLong());
    }

    @Test
    @DisplayName("Deve criar pedido quando parceiro e todos os itens já existirem, sem criar parceiro ou itens")
    void createOrderFromInvoiceSuccess() {
        InvoicePartnerData emitter = InvoicePartnerData.builder()
                .document("44444444000144")
                .name("Fornecedor Exemplo SA")
                .build();
        InvoicePartnerData recipient = InvoicePartnerData.builder()
                .document("11111111000111")
                .name("Minha Empresa Ltda")
                .build();

        InvoiceItemData xmlItem = InvoiceItemData.builder()
                .itemNumber(1)
                .productCode("PROD-10")
                .gtinEan("7891000100103")
                .description("Cabo USB-C")
                .quantity(new BigDecimal("3"))
                .unitPrice(new BigDecimal("45.00"))
                .discountAmount(new BigDecimal("5.00"))
                .build();

        InvoiceCarrierData carrier = InvoiceCarrierData.builder()
                .document("33333333000133")
                .name("Transportadora Rápida")
                .build();

        ParsedInvoiceXml parsedXml = ParsedInvoiceXml.builder()
                .identification(InvoiceIdentificationData.builder().invoiceNumber(1234).build())
                .emitter(emitter)
                .recipient(recipient)
                .items(List.of(xmlItem))
                .totals(InvoiceTotalsData.builder().freightAmount(new BigDecimal("30.00")).build())
                .freight(InvoiceFreightData.builder()
                        .freightModeCode(0)
                        .freightMode(FreightMode.SENDER)
                        .carrier(carrier)
                        .vehiclePlate("ABC1D23")
                        .vehicleState("SP")
                        .volumes(List.of(InvoiceVolumeData.builder()
                                .quantity(2)
                                .species("CAIXA")
                                .grossWeightKg(new BigDecimal("4.500"))
                                .netWeightKg(new BigDecimal("4.000"))
                                .build()))
                        .build())
                .build();

        PartnerResponse partner = buildPartnerResponse(50L, false, true, false, "44444444000144", "Fornecedor Exemplo SA");
        PartnerResponse existingCarrier = buildPartnerResponse(60L, false, false, true, "33333333000133", "Transportadora Rápida");
        CatalogItemResponse catalogItem = buildCatalogItemResponse(700L, "PROD-10", "Cabo USB-C", "45.00");
        OrderResponse openedOrder = OrderResponse.builder()
                .id(900L)
                .companyId(companyId)
                .partnerId(50L)
                .status(OrderStatus.OPEN)
                .totalAmount(new BigDecimal("160.00"))
                .build();

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, parsedXml));
        when(companyService.findById(userPrincipal)).thenReturn(companyResponse);
        when(invoiceXmlReaderService.resolveCounterpartyPartner(parsedXml, "11111111000111"))
                .thenReturn(emitter);
        when(partnerService.findByDocumentAndCompanyId("44444444000144", companyId))
                .thenReturn(Optional.of(partner));
        when(partnerService.findByDocumentAndCompanyId("33333333000133", companyId))
                .thenReturn(Optional.of(existingCarrier));
        when(catalogItemService.findByXmlIdentifiers(companyId, "PROD-10", "7891000100103"))
                .thenReturn(Optional.of(catalogItem));
        when(orderService.openOrder(any(OrderCreateRequest.class), eq(companyId)))
                .thenReturn(openedOrder);

        OrderResponse result = importService.createOrderFromInvoice(
                InvoiceCreateOrderRequest.builder().invoiceId(invoiceId).build(),
                userPrincipal
        );

        assertNotNull(result);
        assertEquals(900L, result.getId());

        ArgumentCaptor<OrderCreateRequest> orderCaptor = ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(orderService, times(1)).openOrder(orderCaptor.capture(), eq(companyId));
        OrderCreateRequest createdReq = orderCaptor.getValue();
        assertEquals(50L, createdReq.getPartnerId());
        assertEquals(1, createdReq.getItems().size());
        assertEquals(700L, createdReq.getItems().get(0).getCatalogItemId());
        assertEquals(new BigDecimal("3"), createdReq.getItems().get(0).getQuantity());
        assertNotNull(createdReq.getShipping());
        assertEquals(60L, createdReq.getShipping().getCarrierId());
        assertEquals(new BigDecimal("30.00"), createdReq.getShipping().getFreightAmount());

        verify(partnerService, never()).save(any(), anyLong());
        verify(catalogItemService, never()).savePhysical(any(), anyLong());
        verify(catalogItemService, never()).saveService(any(), anyLong());
    }

    @Test
    @DisplayName("Não deve criar pedido nem parceiro quando o parceiro do XML não existir no sistema")
    void createOrderAbortWhenPartnerMissing() {
        InvoicePartnerData emitter = InvoicePartnerData.builder()
                .document("44444444000144")
                .name("Fornecedor Inexistente")
                .build();
        ParsedInvoiceXml parsedXml = ParsedInvoiceXml.builder()
                .emitter(emitter)
                .items(List.of(InvoiceItemData.builder()
                        .itemNumber(1)
                        .productCode("PROD-10")
                        .quantity(BigDecimal.ONE)
                        .build()))
                .build();

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, parsedXml));
        when(companyService.findById(userPrincipal)).thenReturn(companyResponse);
        when(invoiceXmlReaderService.resolveCounterpartyPartner(parsedXml, "11111111000111"))
                .thenReturn(emitter);
        when(partnerService.findByDocumentAndCompanyId("44444444000144", companyId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                importService.createOrderFromInvoice(
                        InvoiceCreateOrderRequest.builder().invoiceId(invoiceId).build(),
                        userPrincipal
                )
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(partnerService, never()).save(any(), anyLong());
        verify(catalogItemService, never()).savePhysical(any(), anyLong());
        verify(orderService, never()).openOrder(any(), anyLong());
    }

    @Test
    @DisplayName("Não deve criar pedido nem itens quando algum item do XML não existir no catálogo")
    void createOrderAbortWhenCatalogItemMissing() {
        InvoicePartnerData emitter = InvoicePartnerData.builder()
                .document("44444444000144")
                .name("Fornecedor Exemplo SA")
                .build();
        InvoiceItemData existingItem = InvoiceItemData.builder()
                .itemNumber(1)
                .productCode("PROD-10")
                .quantity(BigDecimal.ONE)
                .build();
        InvoiceItemData missingItem = InvoiceItemData.builder()
                .itemNumber(2)
                .productCode("PROD-MISSING")
                .description("Item Inexistente")
                .quantity(BigDecimal.ONE)
                .build();

        ParsedInvoiceXml parsedXml = ParsedInvoiceXml.builder()
                .emitter(emitter)
                .items(List.of(existingItem, missingItem))
                .build();

        when(invoiceService.loadAndParseXml(invoiceId, companyId))
                .thenReturn(new LoadedInvoiceXml(invoiceEntity, parsedXml));
        when(companyService.findById(userPrincipal)).thenReturn(companyResponse);
        when(invoiceXmlReaderService.resolveCounterpartyPartner(parsedXml, "11111111000111"))
                .thenReturn(emitter);
        when(partnerService.findByDocumentAndCompanyId("44444444000144", companyId))
                .thenReturn(Optional.of(buildPartnerResponse(50L, false, true, false, "44444444000144", "Fornecedor Exemplo SA")));
        when(catalogItemService.findByXmlIdentifiers(companyId, "PROD-10", null))
                .thenReturn(Optional.of(buildCatalogItemResponse(700L, "PROD-10", "Item Existente", "10.00")));
        when(catalogItemService.findByXmlIdentifiers(companyId, "PROD-MISSING", null))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                importService.createOrderFromInvoice(
                        InvoiceCreateOrderRequest.builder().invoiceId(invoiceId).build(),
                        userPrincipal
                )
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("PROD-MISSING"));
        verify(catalogItemService, never()).savePhysical(any(), anyLong());
        verify(partnerService, never()).save(any(), anyLong());
        verify(orderService, never()).openOrder(any(), anyLong());
    }

    private CatalogItemResponse buildCatalogItemResponse(Long id, String internalCode, String description, String price) {
        return new CatalogItemResponse(
                id,
                companyId,
                null,
                CatalogItemType.PHYSICAL,
                internalCode,
                "ACTIVE",
                description,
                new BigDecimal(price),
                "UN",
                new CatalogPhysicalItemResponse(id, "84716052", "5102", "102", "SEM GTIN", null, 0, BigDecimal.ZERO, null, null, null),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private PartnerResponse buildPartnerResponse(
            Long id,
            boolean isCustomer,
            boolean isSupplier,
            boolean isCarrier,
            String document,
            String name
    ) {
        return new PartnerResponse(
                id, companyId, isCustomer, isSupplier, isCarrier, PersonType.PJ,
                document, name, 1, null, null, null, "ACTIVE", null, null,
                null, null, null, null, null, null, "1058", "Brasil",
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
