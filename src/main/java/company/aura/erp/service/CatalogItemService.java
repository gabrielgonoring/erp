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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CatalogItemService {

    private final CatalogItemRepository catalogItemRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public CatalogItemResponse savePhysical(CatalogPhysicalItemCreateRequest request, Long companyId) {
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
        return CatalogItemResponse.fromEntity(saved);
    }

    @Transactional
    public CatalogItemResponse saveService(CatalogServiceItemCreateRequest request, Long companyId) {
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
        return CatalogItemResponse.fromEntity(saved);
    }

    @Transactional
    public CatalogItemResponse updatePhysical(Long id, CatalogPhysicalItemUpdateRequest request, Long companyId) {
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
        return CatalogItemResponse.fromEntity(updated);
    }

    @Transactional
    public CatalogItemResponse updateService(Long id, CatalogServiceItemUpdateRequest request, Long companyId) {
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
        return CatalogItemResponse.fromEntity(updated);
    }


    @Transactional(readOnly = true)
    public CatalogItemResponse findById(Long id, Long companyId) {
        CatalogItemEntity item = catalogItemRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CatalogItemStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do catálogo não encontrado com ID: " + id));

        return CatalogItemResponse.fromEntity(item);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        CatalogItemEntity item = catalogItemRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CatalogItemStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do catálogo não encontrado com ID: " + id));

        item.setStatus(CatalogItemStatus.DELETED.name());
        catalogItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public Page<CatalogItemSummaryResponse> listSummarized(String search, Long companyId, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return catalogItemRepository.searchByCompanyIdAndTerm(companyId, cleanSearch, pageable)
                .map(CatalogItemSummaryResponse::fromEntity);
    }
}
