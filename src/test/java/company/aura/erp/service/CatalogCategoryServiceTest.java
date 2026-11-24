package company.aura.erp.service;

import company.aura.erp.dto.catalog.CatalogCategoryCreateRequest;
import company.aura.erp.dto.catalog.CatalogCategoryResponse;
import company.aura.erp.dto.catalog.CatalogCategorySummaryResponse;
import company.aura.erp.dto.catalog.CatalogCategoryUpdateRequest;
import company.aura.erp.entity.CatalogCategoryEntity;
import company.aura.erp.enums.CatalogCategoryStatus;
import company.aura.erp.repository.CatalogCategoryRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogCategoryServiceTest {

    @Mock
    private CatalogCategoryRepository catalogCategoryRepository;

    @InjectMocks
    private CatalogCategoryService catalogCategoryService;

    @Test
    @DisplayName("Deve criar categoria quando dados são válidos")
    void shouldCreateCategory() {
        CatalogCategoryCreateRequest request = CatalogCategoryCreateRequest.builder()
                .name("  Eletrônicos  ")
                .build();

        when(catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCaseAndStatusNot(10L, "Eletrônicos", CatalogCategoryStatus.DELETED))
                .thenReturn(false);
        when(catalogCategoryRepository.save(any(CatalogCategoryEntity.class))).thenAnswer(inv -> {
            CatalogCategoryEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        CatalogCategoryResponse response = catalogCategoryService.create(request, 10L);

        assertEquals(1L, response.getId());
        assertEquals(10L, response.getCompanyId());
        assertEquals("Eletrônicos", response.getName());
        assertEquals(CatalogCategoryStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve lançar CONFLICT ao criar categoria com nome duplicado")
    void shouldThrowConflictOnDuplicateName() {
        CatalogCategoryCreateRequest request = CatalogCategoryCreateRequest.builder()
                .name("Eletrônicos")
                .build();

        when(catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCaseAndStatusNot(10L, "Eletrônicos", CatalogCategoryStatus.DELETED))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> catalogCategoryService.create(request, 10L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve atualizar categoria existente mantendo status")
    void shouldUpdateCategory() {
        CatalogCategoryEntity entity = CatalogCategoryEntity.builder()
                .id(1L)
                .companyId(10L)
                .name("Eletrônicos")
                .status(CatalogCategoryStatus.ACTIVE)
                .build();
        CatalogCategoryUpdateRequest request = CatalogCategoryUpdateRequest.builder()
                .name("Informática")
                .build();

        when(catalogCategoryRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CatalogCategoryStatus.DELETED))
                .thenReturn(Optional.of(entity));
        when(catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCaseAndStatusNotAndIdNot(10L, "Informática", CatalogCategoryStatus.DELETED, 1L))
                .thenReturn(false);
        when(catalogCategoryRepository.save(entity)).thenReturn(entity);

        CatalogCategoryResponse response = catalogCategoryService.update(1L, request, 10L);

        assertEquals("Informática", response.getName());
        assertEquals(CatalogCategoryStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve realizar soft delete na categoria")
    void shouldSoftDeleteCategory() {
        CatalogCategoryEntity entity = CatalogCategoryEntity.builder()
                .id(1L)
                .companyId(10L)
                .name("Eletrônicos")
                .status(CatalogCategoryStatus.ACTIVE)
                .build();

        when(catalogCategoryRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CatalogCategoryStatus.DELETED))
                .thenReturn(Optional.of(entity));

        catalogCategoryService.delete(1L, 10L);

        assertEquals(CatalogCategoryStatus.DELETED, entity.getStatus());
        verify(catalogCategoryRepository).save(entity);
    }

    @Test
    @DisplayName("Deve buscar categoria por ID")
    void shouldFindCategoryById() {
        CatalogCategoryEntity entity = CatalogCategoryEntity.builder()
                .id(1L)
                .companyId(10L)
                .name("Eletrônicos")
                .status(CatalogCategoryStatus.ACTIVE)
                .build();

        when(catalogCategoryRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CatalogCategoryStatus.DELETED))
                .thenReturn(Optional.of(entity));

        CatalogCategoryResponse response = catalogCategoryService.findById(1L, 10L);

        assertEquals(1L, response.getId());
        assertEquals("Eletrônicos", response.getName());
    }

    @Test
    @DisplayName("Deve listar categorias deletadas quando filtro status for DELETED")
    void shouldListDeletedCategoriesWhenStatusIsDeleted() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CatalogCategorySummaryResponse> expected = new PageImpl<>(
                List.of(new CatalogCategorySummaryResponse(2L, "Antigos", CatalogCategoryStatus.DELETED)),
                pageable,
                1
        );

        when(catalogCategoryRepository.searchSummarized(10L, null, CatalogCategoryStatus.DELETED, pageable))
                .thenReturn(expected);

        Page<CatalogCategorySummaryResponse> page = catalogCategoryService.listSummarized(null, CatalogCategoryStatus.DELETED, 10L, pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals(CatalogCategoryStatus.DELETED, page.getContent().getFirst().getStatus());
    }

    @Test
    @DisplayName("Deve listar categorias resumidas incluindo deletadas quando status não for informado")
    void shouldListSummarizedCategories() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CatalogCategorySummaryResponse> expected = new PageImpl<>(
                List.of(
                        new CatalogCategorySummaryResponse(1L, "Eletrônicos", CatalogCategoryStatus.ACTIVE),
                        new CatalogCategorySummaryResponse(2L, "Eletrônicos Antigos", CatalogCategoryStatus.DELETED)
                ),
                pageable,
                2
        );

        when(catalogCategoryRepository.searchSummarized(10L, "Elet", null, pageable))
                .thenReturn(expected);

        Page<CatalogCategorySummaryResponse> result = catalogCategoryService.listSummarized(" Elet ", null, 10L, pageable);

        assertEquals(2, result.getTotalElements());
    }
}
