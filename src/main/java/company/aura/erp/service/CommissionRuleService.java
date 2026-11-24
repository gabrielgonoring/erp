package company.aura.erp.service;

import company.aura.erp.dto.commission.CommissionRuleCreateRequest;
import company.aura.erp.dto.commission.CommissionRuleResponse;
import company.aura.erp.dto.commission.CommissionRuleSummaryResponse;
import company.aura.erp.dto.commission.CommissionRuleUpdateRequest;
import company.aura.erp.entity.CommissionRuleEntity;
import company.aura.erp.enums.CatalogCategoryStatus;
import company.aura.erp.enums.CatalogItemStatus;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.enums.CommissionRuleStatus;
import company.aura.erp.mapper.CommissionRuleMapper;
import company.aura.erp.repository.CatalogCategoryRepository;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.CommissionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionRuleService {

    private final CommissionRuleRepository commissionRuleRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CatalogCategoryRepository catalogCategoryRepository;
    private final CatalogItemRepository catalogItemRepository;

    @Transactional
    public CommissionRuleResponse create(CommissionRuleCreateRequest request, Long companyId) {
        log.info("create() - START - companyId={}, collaboratorId={}, categoryId={}, catalogItemId={}",
                companyId, request.getCollaboratorId(), request.getCategoryId(), request.getCatalogItemId());

        validateRuleFields(request.getCollaboratorId(), request.getCategoryId(), request.getCatalogItemId(),
                request.getValidFrom(), request.getValidUntil(), companyId);

        CommissionRuleEntity entity = CommissionRuleMapper.toEntity(request, companyId);
        CommissionRuleEntity saved = commissionRuleRepository.save(entity);
        CommissionRuleResponse response = buildResponse(saved, companyId);

        log.info("create() - END - ruleId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public CommissionRuleResponse update(Long id, CommissionRuleUpdateRequest request, Long companyId) {
        log.info("update() - START - id={}, companyId={}", id, companyId);

        CommissionRuleEntity entity = findRule(id, companyId);

        validateRuleFields(request.getCollaboratorId(), request.getCategoryId(), request.getCatalogItemId(),
                request.getValidFrom(), request.getValidUntil(), companyId);

        entity.setCollaboratorId(request.getCollaboratorId());
        entity.setCategoryId(request.getCategoryId());
        entity.setCatalogItemId(request.getCatalogItemId());
        entity.setCommissionPercentage(request.getCommissionPercentage());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidUntil(request.getValidUntil());

        CommissionRuleEntity updated = commissionRuleRepository.save(entity);
        CommissionRuleResponse response = buildResponse(updated, companyId);

        log.info("update() - END - id={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        log.info("delete() - START - id={}, companyId={}", id, companyId);

        CommissionRuleEntity entity = findRule(id, companyId);
        entity.setStatus(CommissionRuleStatus.DELETED);
        commissionRuleRepository.save(entity);

        log.info("delete() - END - id={}, companyId={}", id, companyId);
    }

    @Transactional(readOnly = true)
    public CommissionRuleResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        CommissionRuleEntity entity = findRule(id, companyId);
        CommissionRuleResponse response = buildResponse(entity, companyId);

        log.info("findById() - END - id={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CommissionRuleSummaryResponse> listSummarized(
            String search,
            Long collaboratorId,
            Long categoryId,
            Long catalogItemId,
            CommissionRuleStatus status,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listSummarized() - START - companyId={}, collaboratorId={}, categoryId={}, catalogItemId={}, status={}",
                companyId, collaboratorId, categoryId, catalogItemId, status);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<CommissionRuleSummaryResponse> page = commissionRuleRepository.searchSummarized(
                companyId,
                searchTerm,
                collaboratorId,
                categoryId,
                catalogItemId,
                status,
                pageable
        );

        log.info("listSummarized() - END - companyId={}, totalElements={}", companyId, page.getTotalElements());
        return page;
    }

    private void validateRuleFields(
            Long collaboratorId,
            Long categoryId,
            Long catalogItemId,
            LocalDate validFrom,
            LocalDate validUntil,
            Long companyId
    ) {
        if (categoryId != null && catalogItemId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A regra de comissão não pode ser vinculada simultaneamente a uma categoria e a um item do catálogo.");
        }

        if (validFrom != null && validUntil != null && validFrom.isAfter(validUntil)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data inicial de vigência não pode ser posterior à data final.");
        }

        if (collaboratorId != null) {
            collaboratorRepository.findByIdAndCompanyIdAndStatusNot(collaboratorId, companyId, CollaboratorStatus.DELETED.name())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + collaboratorId));
        }

        if (categoryId != null) {
            catalogCategoryRepository.findByIdAndCompanyIdAndStatusNot(categoryId, companyId, CatalogCategoryStatus.DELETED)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada com ID: " + categoryId));
        }

        if (catalogItemId != null) {
            catalogItemRepository.findByIdAndCompanyIdAndStatusNot(catalogItemId, companyId, CatalogItemStatus.DELETED.name())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do catálogo não encontrado com ID: " + catalogItemId));
        }
    }

    private CommissionRuleEntity findRule(Long id, Long companyId) {
        return commissionRuleRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CommissionRuleStatus.DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Regra de comissão não encontrada com ID: " + id));
    }

    private CommissionRuleResponse buildResponse(CommissionRuleEntity entity, Long companyId) {
        String collaboratorName = entity.getCollaboratorId() != null
                ? collaboratorRepository.findNameByIdAndCompanyId(entity.getCollaboratorId(), companyId).orElse(null)
                : null;
        String categoryName = entity.getCategoryId() != null
                ? catalogCategoryRepository.findNameByIdAndCompanyId(entity.getCategoryId(), companyId).orElse(null)
                : null;
        String catalogItemDescription = entity.getCatalogItemId() != null
                ? catalogItemRepository.findByIdAndCompanyId(entity.getCatalogItemId(), companyId)
                        .map(item -> item.getDescription())
                        .orElse(null)
                : null;

        return CommissionRuleMapper.fromEntity(entity, collaboratorName, categoryName, catalogItemDescription);
    }
}
