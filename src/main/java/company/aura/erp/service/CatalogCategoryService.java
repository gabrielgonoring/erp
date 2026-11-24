package company.aura.erp.service;

import company.aura.erp.dto.catalog.CatalogCategoryCreateRequest;
import company.aura.erp.dto.catalog.CatalogCategoryResponse;
import company.aura.erp.dto.catalog.CatalogCategorySummaryResponse;
import company.aura.erp.dto.catalog.CatalogCategoryUpdateRequest;
import company.aura.erp.entity.CatalogCategoryEntity;
import company.aura.erp.enums.CatalogCategoryStatus;
import company.aura.erp.mapper.CatalogCategoryMapper;
import company.aura.erp.repository.CatalogCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogCategoryService {

    private final CatalogCategoryRepository catalogCategoryRepository;

    @Transactional
    public CatalogCategoryResponse create(CatalogCategoryCreateRequest request, Long companyId) {
        log.info("create() - START - companyId={}, name={}", companyId, request.getName());

        String normalizedName = request.getName().trim();
        if (catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCaseAndStatusNot(companyId, normalizedName, CatalogCategoryStatus.DELETED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma categoria com o nome: " + normalizedName);
        }

        CatalogCategoryEntity entity = CatalogCategoryMapper.toEntity(request, companyId);
        CatalogCategoryEntity saved = catalogCategoryRepository.save(entity);
        CatalogCategoryResponse response = CatalogCategoryMapper.fromEntity(saved);

        log.info("create() - END - categoryId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public CatalogCategoryResponse update(Long id, CatalogCategoryUpdateRequest request, Long companyId) {
        log.info("update() - START - id={}, companyId={}, name={}", id, companyId, request.getName());

        CatalogCategoryEntity entity = findCategory(id, companyId);

        String normalizedName = request.getName().trim();
        if (catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCaseAndStatusNotAndIdNot(companyId, normalizedName, CatalogCategoryStatus.DELETED, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma categoria com o nome: " + normalizedName);
        }

        entity.setName(normalizedName);

        CatalogCategoryEntity updated = catalogCategoryRepository.save(entity);
        CatalogCategoryResponse response = CatalogCategoryMapper.fromEntity(updated);

        log.info("update() - END - id={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        log.info("delete() - START - id={}, companyId={}", id, companyId);

        CatalogCategoryEntity entity = findCategory(id, companyId);
        entity.setStatus(CatalogCategoryStatus.DELETED);
        catalogCategoryRepository.save(entity);

        log.info("delete() - END - id={}, companyId={}", id, companyId);
    }

    @Transactional(readOnly = true)
    public CatalogCategoryResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        CatalogCategoryEntity entity = findCategory(id, companyId);
        CatalogCategoryResponse response = CatalogCategoryMapper.fromEntity(entity);

        log.info("findById() - END - id={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CatalogCategorySummaryResponse> listSummarized(
            String search,
            CatalogCategoryStatus status,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listSummarized() - START - companyId={}, search={}, status={}", companyId, search, status);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<CatalogCategorySummaryResponse> page = catalogCategoryRepository.searchSummarized(
                companyId,
                searchTerm,
                status,
                pageable
        );

        log.info("listSummarized() - END - companyId={}, totalElements={}", companyId, page.getTotalElements());
        return page;
    }

    private CatalogCategoryEntity findCategory(Long id, Long companyId) {
        return catalogCategoryRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CatalogCategoryStatus.DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada com ID: " + id));
    }
}
