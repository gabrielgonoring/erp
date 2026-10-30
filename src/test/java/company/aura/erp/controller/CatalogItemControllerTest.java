package company.aura.erp.controller;

import company.aura.erp.dto.catalog.*;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CatalogItemService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogItemControllerTest {

    @Mock
    private CatalogItemService catalogItemService;

    @InjectMocks
    private CatalogItemController catalogItemController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void shouldCreatePhysicalCatalogItemAndReturnCreated() {
        CatalogPhysicalItemCreateRequest request = new CatalogPhysicalItemCreateRequest(
                "PROD-001", "Monitor", new BigDecimal("1200.00"), "UN",
                "85285200", "5102", "102", "SEM GTIN", null, 0,
                BigDecimal.ZERO, null, null, null
        );

        CatalogItemResponse expectedResponse = new CatalogItemResponse(
                1L, 10L, CatalogItemType.PHYSICAL, "PROD-001", "ACTIVE", "Monitor",
                new BigDecimal("1200.00"), "UN", null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(catalogItemService.savePhysical(request, 10L)).thenReturn(expectedResponse);

        ResponseEntity<CatalogItemResponse> response = catalogItemController.createPhysical(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(catalogItemService).savePhysical(request, 10L);
    }

    @Test
    void shouldCreateServiceCatalogItemAndReturnCreated() {
        CatalogServiceItemCreateRequest request = new CatalogServiceItemCreateRequest(
                "SRV-001", "Consultoria", new BigDecimal("200.00"), "HR",
                "01.07", "123", new BigDecimal("5.00"), false
        );

        CatalogItemResponse expectedResponse = new CatalogItemResponse(
                2L, 10L, CatalogItemType.SERVICE, "SRV-001", "ACTIVE", "Consultoria",
                new BigDecimal("200.00"), "HR", null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(catalogItemService.saveService(request, 10L)).thenReturn(expectedResponse);

        ResponseEntity<CatalogItemResponse> response = catalogItemController.createService(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2L, response.getBody().id());
        verify(catalogItemService).saveService(request, 10L);
    }

    @Test
    void shouldUpdatePhysicalCatalogItemAndReturnOk() {
        CatalogPhysicalItemUpdateRequest request = new CatalogPhysicalItemUpdateRequest(
                "PROD-001-ALT", "Monitor Alterado", new BigDecimal("1300.00"), "UN",
                "85285200", "5102", "102", "SEM GTIN", null, 0,
                BigDecimal.ZERO, null, null, null
        );

        CatalogItemResponse expectedResponse = new CatalogItemResponse(
                1L, 10L, CatalogItemType.PHYSICAL, "PROD-001-ALT", "ACTIVE", "Monitor Alterado",
                new BigDecimal("1300.00"), "UN", null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(catalogItemService.updatePhysical(1L, request, 10L)).thenReturn(expectedResponse);

        ResponseEntity<CatalogItemResponse> response = catalogItemController.updatePhysical(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Monitor Alterado", response.getBody().description());
        verify(catalogItemService).updatePhysical(1L, request, 10L);
    }

    @Test
    void shouldUpdateServiceCatalogItemAndReturnOk() {
        CatalogServiceItemUpdateRequest request = new CatalogServiceItemUpdateRequest(
                "SRV-001-ALT", "Consultoria Avançada", new BigDecimal("350.00"), "HR",
                "01.07", "123", new BigDecimal("5.00"), true
        );

        CatalogItemResponse expectedResponse = new CatalogItemResponse(
                2L, 10L, CatalogItemType.SERVICE, "SRV-001-ALT", "ACTIVE", "Consultoria Avançada",
                new BigDecimal("350.00"), "HR", null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(catalogItemService.updateService(2L, request, 10L)).thenReturn(expectedResponse);

        ResponseEntity<CatalogItemResponse> response = catalogItemController.updateService(2L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Consultoria Avançada", response.getBody().description());
        verify(catalogItemService).updateService(2L, request, 10L);
    }

    @Test
    void shouldFindByIdAndReturnOk() {
        CatalogItemResponse expectedResponse = new CatalogItemResponse(
                1L, 10L, CatalogItemType.PHYSICAL, "PROD-001", "ACTIVE", "Monitor",
                new BigDecimal("1200.00"), "UN", null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(catalogItemService.findById(1L, 10L)).thenReturn(expectedResponse);

        ResponseEntity<CatalogItemResponse> response = catalogItemController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(catalogItemService).findById(1L, 10L);
    }

    @Test
    void shouldListSummarizedAndReturnOk() {
        Pageable pageable = PageRequest.of(0, 20);
        CatalogItemSummaryResponse summary = new CatalogItemSummaryResponse(
                1L, "PROD-001", "Monitor", "Monitor", new BigDecimal("1200.00")
        );
        Page<CatalogItemSummaryResponse> page = new PageImpl<>(List.of(summary));

        when(catalogItemService.listSummarized("Monitor", 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<CatalogItemSummaryResponse>> response = catalogItemController.list("Monitor", pageable, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(catalogItemService).listSummarized("Monitor", 10L, pageable);
    }

    @Test
    void shouldDeleteAndReturnNoContent() {
        ResponseEntity<Void> response = catalogItemController.delete(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(catalogItemService).delete(1L, 10L);
    }
}
