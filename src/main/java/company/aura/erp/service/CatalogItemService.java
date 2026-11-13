package company.aura.erp.service;

import company.aura.erp.dto.catalog.*;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.CatalogPhysicalItemEntity;
import company.aura.erp.entity.CatalogServiceItemEntity;
import company.aura.erp.enums.CatalogItemStatus;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogItemService {

    private final CatalogItemRepository catalogItemRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public CatalogItemResponse savePhysical(CatalogPhysicalItemCreateRequest request, Long companyId) {
        log.info("savePhysical() - START - companyId={}, internalCode={}, description={}", companyId, request != null ? request.internalCode() : null, request != null ? request.description() : null);

        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada com ID: " + companyId);
        }

        CatalogItemEntity item = CatalogItemEntity.builder()
                .companyId(companyId)
                .type(CatalogItemType.PHYSICAL)
                .internalCode(request.internalCode())
                .description(request.description())
                .unitPrice(request.unitPrice())
                .unitOfMeasure(request.unitOfMeasure())
                .status(CatalogItemStatus.ACTIVE.name())
                .build();

        CatalogPhysicalItemEntity physicalItem = CatalogPhysicalItemEntity.builder()
                .catalogItem(item)
                .ncm(request.ncm())
                .defaultCfop(request.defaultCfop())
                .defaultCsosn(request.defaultCsosn() != null && !request.defaultCsosn().isBlank()
                        ? request.defaultCsosn() : "102")
                .gtinEan(request.gtinEan() != null && !request.gtinEan().isBlank()
                        ? request.gtinEan() : "SEM GTIN")
                .cest(request.cest())
                .origin(request.origin() != null ? request.origin() : 0)
                .defaultIcmsCreditRate(request.defaultIcmsCreditRate() != null
                        ? request.defaultIcmsCreditRate() : BigDecimal.ZERO)
                .icmsRate(request.icmsRate())
                .pisRate(request.pisRate())
                .cofinsRate(request.cofinsRate())
                .build();

        item.setPhysicalItem(physicalItem);

        CatalogItemEntity saved = catalogItemRepository.save(item);
        CatalogItemResponse response = CatalogItemResponse.fromEntity(saved);

        log.info("savePhysical() - END - id={}, companyId={}", response.id(), companyId);
        return response;
    }

    @Transactional
    public CatalogItemResponse saveService(CatalogServiceItemCreateRequest request, Long companyId) {
        log.info("saveService() - START - companyId={}, internalCode={}, description={}", companyId, request != null ? request.internalCode() : null, request != null ? request.description() : null);

        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada com ID: " + companyId);
        }

        CatalogItemEntity item = CatalogItemEntity.builder()
                .companyId(companyId)
                .type(CatalogItemType.SERVICE)
                .internalCode(request.internalCode())
                .description(request.description())
                .unitPrice(request.unitPrice())
                .unitOfMeasure(request.unitOfMeasure())
                .status(CatalogItemStatus.ACTIVE.name())
                .build();

        CatalogServiceItemEntity serviceItem = CatalogServiceItemEntity.builder()
                .catalogItem(item)
                .lc116ServiceCode(request.lc116ServiceCode())
                .cityTaxCode(request.cityTaxCode())
                .issRate(request.issRate() != null ? request.issRate() : BigDecimal.ZERO)
                .issWithheld(Boolean.TRUE.equals(request.issWithheld()))
                .build();

        item.setServiceItem(serviceItem);

        CatalogItemEntity saved = catalogItemRepository.save(item);
        CatalogItemResponse response = CatalogItemResponse.fromEntity(saved);

        log.info("saveService() - END - id={}, companyId={}", response.id(), companyId);
        return response;
    }

    @Transactional
    public CatalogItemResponse updatePhysical(Long id, CatalogPhysicalItemUpdateRequest request, Long companyId) {
        log.info("updatePhysical() - START - id={}, companyId={}, internalCode={}", id, companyId, request != null ? request.internalCode() : null);

        CatalogItemEntity item = catalogItemRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CatalogItemStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do catálogo não encontrado com ID: " + id));

        if (item.getType() != CatalogItemType.PHYSICAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O item com ID " + id + " é do tipo SERVIÇO e não pode ser atualizado como produto físico.");
        }

        item.setInternalCode(request.internalCode());
        item.setDescription(request.description());
        item.setUnitPrice(request.unitPrice());
        item.setUnitOfMeasure(request.unitOfMeasure());

        CatalogPhysicalItemEntity physicalItem = item.getPhysicalItem();
        if (physicalItem == null) {
            physicalItem = new CatalogPhysicalItemEntity();
            physicalItem.setCatalogItem(item);
            item.setPhysicalItem(physicalItem);
        }

        physicalItem.setNcm(request.ncm());
        physicalItem.setDefaultCfop(request.defaultCfop());
        physicalItem.setDefaultCsosn(request.defaultCsosn() != null && !request.defaultCsosn().isBlank()
                ? request.defaultCsosn() : "102");
        physicalItem.setGtinEan(request.gtinEan() != null && !request.gtinEan().isBlank()
                ? request.gtinEan() : "SEM GTIN");
        physicalItem.setCest(request.cest());
        physicalItem.setOrigin(request.origin() != null ? request.origin() : 0);
        physicalItem.setDefaultIcmsCreditRate(request.defaultIcmsCreditRate() != null
                ? request.defaultIcmsCreditRate() : BigDecimal.ZERO);
        physicalItem.setIcmsRate(request.icmsRate());
        physicalItem.setPisRate(request.pisRate());
        physicalItem.setCofinsRate(request.cofinsRate());

        CatalogItemEntity updated = catalogItemRepository.save(item);
        CatalogItemResponse response = CatalogItemResponse.fromEntity(updated);

        log.info("updatePhysical() - END - id={}, companyId={}", response.id(), companyId);
        return response;
    }

    @Transactional
    public CatalogItemResponse updateService(Long id, CatalogServiceItemUpdateRequest request, Long companyId) {
        log.info("updateService() - START - id={}, companyId={}, internalCode={}", id, companyId, request != null ? request.internalCode() : null);

        CatalogItemEntity item = catalogItemRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CatalogItemStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do catálogo não encontrado com ID: " + id));

        if (item.getType() != CatalogItemType.SERVICE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O item com ID " + id + " é do tipo FÍSICO e não pode ser atualizado como serviço.");
        }

        item.setInternalCode(request.internalCode());
        item.setDescription(request.description());
        item.setUnitPrice(request.unitPrice());
        item.setUnitOfMeasure(request.unitOfMeasure());

        CatalogServiceItemEntity serviceItem = item.getServiceItem();
        if (serviceItem == null) {
            serviceItem = new CatalogServiceItemEntity();
            serviceItem.setCatalogItem(item);
            item.setServiceItem(serviceItem);
        }

        serviceItem.setLc116ServiceCode(request.lc116ServiceCode());
        serviceItem.setCityTaxCode(request.cityTaxCode());
        serviceItem.setIssRate(request.issRate() != null ? request.issRate() : BigDecimal.ZERO);
        serviceItem.setIssWithheld(Boolean.TRUE.equals(request.issWithheld()));

        CatalogItemEntity updated = catalogItemRepository.save(item);
        CatalogItemResponse response = CatalogItemResponse.fromEntity(updated);

        log.info("updateService() - END - id={}, companyId={}", response.id(), companyId);
        return response;
    }

    @Transactional(readOnly = true)
    public CatalogItemResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        CatalogItemEntity item = catalogItemRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CatalogItemStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do catálogo não encontrado com ID: " + id));

        CatalogItemResponse response = CatalogItemResponse.fromEntity(item);

        log.info("findById() - END - id={}, companyId={}", response.id(), companyId);
        return response;
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        log.info("delete() - START - id={}, companyId={}", id, companyId);

        CatalogItemEntity item = catalogItemRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CatalogItemStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do catálogo não encontrado com ID: " + id));

        item.setStatus(CatalogItemStatus.DELETED.name());
        catalogItemRepository.save(item);

        log.info("delete() - END - id={}, companyId={}, status={}", id, companyId, item.getStatus());
    }

    @Transactional(readOnly = true)
    public Page<CatalogItemSummaryResponse> listSummarized(String search, Long companyId, Pageable pageable) {
        log.info("listSummarized() - START - companyId={}, search={}", companyId, search);

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<CatalogItemSummaryResponse> page = catalogItemRepository.searchByCompanyIdAndTerm(companyId, cleanSearch, pageable)
                .map(CatalogItemSummaryResponse::fromEntity);

        log.info("listSummarized() - END - companyId={}, totalElements={}, totalPages={}", companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }
}
