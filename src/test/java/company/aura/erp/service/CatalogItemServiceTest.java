package company.aura.erp.service;

import company.aura.erp.dto.catalog.*;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.CatalogPhysicalItemEntity;
import company.aura.erp.entity.CatalogServiceItemEntity;
import company.aura.erp.enums.CatalogItemStatus;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogItemServiceTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CatalogItemService catalogItemService;

    private CatalogItemEntity activePhysicalItem;
    private CatalogItemEntity activeServiceItem;

    @BeforeEach
    void setUp() {
        activePhysicalItem = CatalogItemEntity.builder()
                .id(1L)
                .companyId(10L)
                .type(CatalogItemType.PHYSICAL)
                .internalCode("PROD-001")
                .description("Monitor Gamer 27")
                .unitPrice(new BigDecimal("1500.00"))
                .unitOfMeasure("UN")
                .status(CatalogItemStatus.ACTIVE.name())
                .build();

        CatalogPhysicalItemEntity physicalEntity = CatalogPhysicalItemEntity.builder()
                .catalogItemId(1L)
                .catalogItem(activePhysicalItem)
                .ncm("85285200")
                .defaultCfop("5102")
                .defaultCsosn("102")
                .gtinEan("7891234567890")
                .origin(0)
                .build();
        activePhysicalItem.setPhysicalItem(physicalEntity);

        activeServiceItem = CatalogItemEntity.builder()
                .id(2L)
                .companyId(10L)
                .type(CatalogItemType.SERVICE)
                .internalCode("SRV-001")
                .description("Desenvolvimento de Software")
                .unitPrice(new BigDecimal("250.00"))
                .unitOfMeasure("HR")
                .status(CatalogItemStatus.ACTIVE.name())
                .build();

        CatalogServiceItemEntity serviceEntity = CatalogServiceItemEntity.builder()
                .catalogItemId(2L)
                .catalogItem(activeServiceItem)
                .lc116ServiceCode("01.07")
                .issRate(new BigDecimal("5.00"))
                .issWithheld(false)
                .build();
        activeServiceItem.setServiceItem(serviceEntity);
    }

    @Test
    void shouldSavePhysicalDirectly() {
        when(companyRepository.existsById(10L)).thenReturn(true);
        when(catalogItemRepository.save(any(CatalogItemEntity.class)))
                .thenAnswer(invocation -> {
                    CatalogItemEntity saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        CatalogPhysicalItemCreateRequest request = new CatalogPhysicalItemCreateRequest(
                "PROD-001", "Monitor Gamer 27", new BigDecimal("1500.00"), "UN",
                "85285200", "5102", "102", "7891234567890", null, 0,
                BigDecimal.ZERO, null, null, null
        );

        CatalogItemResponse response = catalogItemService.savePhysical(request, 10L);

        assertNotNull(response);
        assertEquals(CatalogItemType.PHYSICAL, response.type());
        assertEquals("ACTIVE", response.status());
        assertNotNull(response.physicalItem());
        assertEquals("85285200", response.physicalItem().ncm());
        verify(catalogItemRepository).save(any(CatalogItemEntity.class));
    }

    @Test
    void shouldSaveServiceDirectly() {
        when(companyRepository.existsById(10L)).thenReturn(true);
        when(catalogItemRepository.save(any(CatalogItemEntity.class)))
                .thenAnswer(invocation -> {
                    CatalogItemEntity saved = invocation.getArgument(0);
                    saved.setId(2L);
                    return saved;
                });

        CatalogServiceItemCreateRequest request = new CatalogServiceItemCreateRequest(
                "SRV-001", "Desenvolvimento de Software", new BigDecimal("250.00"), "HR",
                "01.07", "12345", new BigDecimal("5.00"), false
        );

        CatalogItemResponse response = catalogItemService.saveService(request, 10L);

        assertNotNull(response);
        assertEquals(CatalogItemType.SERVICE, response.type());
        assertEquals("ACTIVE", response.status());
        assertNotNull(response.serviceItem());
        assertEquals("01.07", response.serviceItem().lc116ServiceCode());
        verify(catalogItemRepository).save(any(CatalogItemEntity.class));
    }

    @Test
    void shouldUpdatePhysicalDirectly() {
        when(catalogItemRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CatalogItemStatus.DELETED.name()))
                .thenReturn(Optional.of(activePhysicalItem));
        when(catalogItemRepository.save(any(CatalogItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CatalogPhysicalItemUpdateRequest request = new CatalogPhysicalItemUpdateRequest(
                "PROD-001-ALT", "Monitor Gamer 27 Atualizado", new BigDecimal("1600.00"), "UN",
                "85285200", "5102", "102", "SEM GTIN", null, 0,
                BigDecimal.ZERO, null, null, null
        );

        CatalogItemResponse response = catalogItemService.updatePhysical(1L, request, 10L);

        assertNotNull(response);
        assertEquals("Monitor Gamer 27 Atualizado", response.description());
        assertEquals(new BigDecimal("1600.00"), response.unitPrice());
        verify(catalogItemRepository).save(activePhysicalItem);
    }

    @Test
    void shouldThrowBadRequestWhenUpdatingServiceItemWithPhysicalUpdate() {
        when(catalogItemRepository.findByIdAndCompanyIdAndStatusNot(2L, 10L, CatalogItemStatus.DELETED.name()))
                .thenReturn(Optional.of(activeServiceItem));

        CatalogPhysicalItemUpdateRequest request = new CatalogPhysicalItemUpdateRequest(
                "SRV-001", "Monitor", new BigDecimal("100.00"), "UN",
                "85285200", "5102", "102", "SEM GTIN", null, 0,
                BigDecimal.ZERO, null, null, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                catalogItemService.updatePhysical(2L, request, 10L)
        );
        assertTrue(ex.getMessage().contains("é do tipo SERVIÇO e não pode ser atualizado como produto físico"));
        verify(catalogItemRepository, never()).save(any());
    }

    @Test
    void shouldUpdateServiceDirectly() {
        when(catalogItemRepository.findByIdAndCompanyIdAndStatusNot(2L, 10L, CatalogItemStatus.DELETED.name()))
                .thenReturn(Optional.of(activeServiceItem));
        when(catalogItemRepository.save(any(CatalogItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CatalogServiceItemUpdateRequest request = new CatalogServiceItemUpdateRequest(
                "SRV-001-ALT", "Desenvolvimento Web", new BigDecimal("300.00"), "HR",
                "01.08", "54321", new BigDecimal("4.00"), true
        );

        CatalogItemResponse response = catalogItemService.updateService(2L, request, 10L);

        assertNotNull(response);
        assertEquals("Desenvolvimento Web", response.description());
        assertEquals("01.08", response.serviceItem().lc116ServiceCode());
        verify(catalogItemRepository).save(activeServiceItem);
    }

    @Test
    void shouldThrowBadRequestWhenUpdatingPhysicalItemWithServiceUpdate() {
        when(catalogItemRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CatalogItemStatus.DELETED.name()))
                .thenReturn(Optional.of(activePhysicalItem));

        CatalogServiceItemUpdateRequest request = new CatalogServiceItemUpdateRequest(
                "PROD-001", "Serviço", new BigDecimal("100.00"), "HR",
                "01.07", null, new BigDecimal("5.00"), false
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                catalogItemService.updateService(1L, request, 10L)
        );
        assertTrue(ex.getMessage().contains("é do tipo FÍSICO e não pode ser atualizado como serviço"));
        verify(catalogItemRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenCompanyDoesNotExistOnCreate() {
        when(companyRepository.existsById(99L)).thenReturn(false);

        CatalogPhysicalItemCreateRequest request = new CatalogPhysicalItemCreateRequest(
                "PROD-001", "Monitor", new BigDecimal("100.00"), "UN",
                "85285200", "5102", "102", "SEM GTIN", null, 0,
                BigDecimal.ZERO, null, null, null
        );

        assertThrows(ResponseStatusException.class, () ->
                catalogItemService.savePhysical(request, 99L)
        );
    }

    @Test
    void shouldFindCatalogItemById() {
        when(catalogItemRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CatalogItemStatus.DELETED.name()))
                .thenReturn(Optional.of(activePhysicalItem));

        CatalogItemResponse response = catalogItemService.findById(1L, 10L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Monitor Gamer 27", response.description());
        assertNotNull(response.physicalItem());
    }

    @Test
    void shouldThrowNotFoundWhenItemDoesNotExistOrIsDeleted() {
        when(catalogItemRepository.findByIdAndCompanyIdAndStatusNot(99L, 10L, CatalogItemStatus.DELETED.name()))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                catalogItemService.findById(99L, 10L)
        );
    }

    @Test
    void shouldDeleteCatalogItemByChangingStatusToDeleted() {
        when(catalogItemRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CatalogItemStatus.DELETED.name()))
                .thenReturn(Optional.of(activePhysicalItem));
        when(catalogItemRepository.save(any(CatalogItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        catalogItemService.delete(1L, 10L);

        assertEquals(CatalogItemStatus.DELETED.name(), activePhysicalItem.getStatus());
        verify(catalogItemRepository).save(activePhysicalItem);
        verify(catalogItemRepository, never()).delete(any());
        verify(catalogItemRepository, never()).deleteById(any());
    }

    @Test
    void shouldListSummarizedCatalogItemsCallingRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CatalogItemEntity> page = new PageImpl<>(List.of(activePhysicalItem));

        when(catalogItemRepository.searchByCompanyIdAndTerm(eq(10L), eq("Monitor"), eq(pageable)))
                .thenReturn(page);

        Page<CatalogItemSummaryResponse> result = catalogItemService.listSummarized("Monitor", 10L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Monitor Gamer 27", result.getContent().get(0).name());
        assertEquals("Monitor Gamer 27", result.getContent().get(0).description());
        assertEquals("PROD-001", result.getContent().get(0).internalCode());
    }
}
