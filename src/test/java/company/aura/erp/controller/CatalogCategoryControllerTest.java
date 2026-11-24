package company.aura.erp.controller;

import company.aura.erp.dto.catalog.CatalogCategoryCreateRequest;
import company.aura.erp.dto.catalog.CatalogCategoryResponse;
import company.aura.erp.dto.catalog.CatalogCategorySummaryResponse;
import company.aura.erp.dto.catalog.CatalogCategoryUpdateRequest;
import company.aura.erp.enums.CatalogCategoryStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CatalogCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogCategoryControllerTest {

    @Mock
    private CatalogCategoryService catalogCategoryService;

    @InjectMocks
    private CatalogCategoryController catalogCategoryController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("admin@empresa.com")
                .name("Admin")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("POST /api/catalog/categories — Deve cadastrar categoria e retornar HTTP 201")
    void shouldCreateCategory() {
        CatalogCategoryCreateRequest request = CatalogCategoryCreateRequest.builder()
                .name("Eletrônicos")
                .build();
        CatalogCategoryResponse expected = CatalogCategoryResponse.builder()
                .id(1L)
                .companyId(10L)
                .name("Eletrônicos")
                .status(CatalogCategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(catalogCategoryService.create(request, 10L)).thenReturn(expected);

        ResponseEntity<CatalogCategoryResponse> response = catalogCategoryController.create(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(catalogCategoryService).create(request, 10L);
    }

    @Test
    @DisplayName("PUT /api/catalog/categories/{id} — Deve editar categoria e retornar HTTP 200")
    void shouldUpdateCategory() {
        CatalogCategoryUpdateRequest request = CatalogCategoryUpdateRequest.builder()
                .name("Informática")
                .build();
        CatalogCategoryResponse expected = CatalogCategoryResponse.builder()
                .id(1L)
                .companyId(10L)
                .name("Informática")
                .status(CatalogCategoryStatus.ACTIVE)
                .build();

        when(catalogCategoryService.update(1L, request, 10L)).thenReturn(expected);

        ResponseEntity<CatalogCategoryResponse> response = catalogCategoryController.update(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(catalogCategoryService).update(1L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/catalog/categories/{id} — Deve deletar categoria e retornar HTTP 204")
    void shouldDeleteCategory() {
        ResponseEntity<Void> response = catalogCategoryController.delete(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(catalogCategoryService).delete(1L, 10L);
    }

    @Test
    @DisplayName("GET /api/catalog/categories/{id} — Deve buscar categoria por ID")
    void shouldFindCategoryById() {
        CatalogCategoryResponse expected = CatalogCategoryResponse.builder()
                .id(1L)
                .companyId(10L)
                .name("Eletrônicos")
                .status(CatalogCategoryStatus.ACTIVE)
                .build();

        when(catalogCategoryService.findById(1L, 10L)).thenReturn(expected);

        ResponseEntity<CatalogCategoryResponse> response = catalogCategoryController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(catalogCategoryService).findById(1L, 10L);
    }

    @Test
    @DisplayName("GET /api/catalog/categories — Deve listar categorias de forma paginada")
    void shouldListCategories() {
        Pageable pageable = PageRequest.of(0, 20);
        CatalogCategorySummaryResponse summary = new CatalogCategorySummaryResponse(1L, "Eletrônicos", CatalogCategoryStatus.ACTIVE);
        Page<CatalogCategorySummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(catalogCategoryService.listSummarized("Elet", CatalogCategoryStatus.ACTIVE, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<CatalogCategorySummaryResponse>> response = catalogCategoryController.list(
                "Elet", CatalogCategoryStatus.ACTIVE, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(catalogCategoryService).listSummarized("Elet", CatalogCategoryStatus.ACTIVE, 10L, pageable);
    }
}
