package company.aura.erp.service;

import company.aura.erp.dto.company.CompanyTermCreateRequest;
import company.aura.erp.dto.company.CompanyTermResponse;
import company.aura.erp.dto.company.CompanyTermSummaryResponse;
import company.aura.erp.dto.company.CompanyTermUpdateRequest;
import company.aura.erp.entity.CompanyTermEntity;
import company.aura.erp.enums.CompanyTermStatus;
import company.aura.erp.mapper.CompanyTermMapper;
import company.aura.erp.repository.CompanyTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyTermService {

    private final CompanyTermRepository companyTermRepository;

    @Transactional(readOnly = true)
    public Page<CompanyTermSummaryResponse> listSummarized(
            String search,
            CompanyTermStatus status,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listSummarized() - START - companyId={}, search={}, status={}", companyId, search, status);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        if (CompanyTermStatus.DELETED.equals(status)) {
            return Page.empty(pageable);
        }

        Page<CompanyTermSummaryResponse> page = companyTermRepository.searchSummarized(
                companyId,
                CompanyTermStatus.DELETED,
                searchTerm,
                status,
                pageable
        );

        log.info("listSummarized() - END - companyId={}, totalElements={}, totalPages={}",
                companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public CompanyTermResponse findById(Long termId, Long companyId) {
        log.info("findById() - START - termId={}, companyId={}", termId, companyId);

        CompanyTermEntity term = findTerm(termId, companyId);
        CompanyTermResponse response = CompanyTermMapper.fromEntity(term);

        log.info("findById() - END - termId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public CompanyTermResponse create(CompanyTermCreateRequest request, Long companyId) {
        log.info("create() - START - companyId={}, title={}", companyId, request.getTitle());

        CompanyTermEntity term = CompanyTermMapper.toEntity(request, companyId);

        if (Boolean.TRUE.equals(term.getIsDefault())) {
            clearDefaultTerms(companyId, null);
        }

        CompanyTermEntity saved = companyTermRepository.save(term);
        CompanyTermResponse response = CompanyTermMapper.fromEntity(saved);

        log.info("create() - END - termId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public CompanyTermResponse update(Long termId, CompanyTermUpdateRequest request, Long companyId) {
        log.info("update() - START - termId={}, companyId={}", termId, companyId);

        CompanyTermEntity term = findTerm(termId, companyId);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultTerms(companyId, term.getId());
        }

        if (request.getIsDefault() != null) {
            term.setIsDefault(request.getIsDefault());
        }

        term.setTitle(request.getTitle());
        term.setContent(request.getContent());

        CompanyTermEntity updated = companyTermRepository.save(term);
        CompanyTermResponse response = CompanyTermMapper.fromEntity(updated);

        log.info("update() - END - termId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public void delete(Long termId, Long companyId) {
        log.info("delete() - START - termId={}, companyId={}", termId, companyId);

        CompanyTermEntity term = findTerm(termId, companyId);
        term.setStatus(CompanyTermStatus.DELETED);
        term.setIsDefault(false);

        companyTermRepository.save(term);

        log.info("delete() - END - termId={}, companyId={}", termId, companyId);
    }

    @Transactional(readOnly = true)
    public void validateTermExists(Long termId, Long companyId) {
        if (termId == null) {
            return;
        }
        findTerm(termId, companyId);
    }

    private CompanyTermEntity findTerm(Long termId, Long companyId) {
        return companyTermRepository.findByIdAndCompanyIdAndStatusNot(termId, companyId, CompanyTermStatus.DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Termo de condição não encontrado"));
    }

    private void clearDefaultTerms(Long companyId, Long excludeId) {
        List<CompanyTermEntity> currentDefaults = companyTermRepository.findByCompanyIdAndIsDefaultTrue(companyId);

        for (CompanyTermEntity existing : currentDefaults) {
            if (!existing.getId().equals(excludeId)) {
                existing.setIsDefault(false);
                companyTermRepository.save(existing);
            }
        }
    }
}
