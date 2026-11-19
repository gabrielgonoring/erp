package company.aura.erp.service;

import company.aura.erp.dto.serviceorder.ServiceOrderTermCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderTermResponse;
import company.aura.erp.dto.serviceorder.ServiceOrderTermSummaryResponse;
import company.aura.erp.dto.serviceorder.ServiceOrderTermUpdateRequest;
import company.aura.erp.entity.ServiceOrderTermEntity;
import company.aura.erp.enums.ServiceOrderTermStatus;
import company.aura.erp.mapper.ServiceOrderTermMapper;
import company.aura.erp.repository.ServiceOrderTermRepository;
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
public class ServiceOrderTermService {

    private final ServiceOrderTermRepository serviceOrderTermRepository;

    @Transactional(readOnly = true)
    public Page<ServiceOrderTermSummaryResponse> listSummarized(
            String search,
            ServiceOrderTermStatus status,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listSummarized() - START - companyId={}, search={}, status={}", companyId, search, status);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        if (ServiceOrderTermStatus.DELETED.equals(status)) {
            return Page.empty(pageable);
        }

        Page<ServiceOrderTermSummaryResponse> page = serviceOrderTermRepository.searchSummarized(
                companyId,
                ServiceOrderTermStatus.DELETED,
                searchTerm,
                status,
                pageable
        );

        log.info("listSummarized() - END - companyId={}, totalElements={}, totalPages={}",
                companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public ServiceOrderTermResponse findById(Long termId, Long companyId) {
        log.info("findById() - START - termId={}, companyId={}", termId, companyId);

        ServiceOrderTermEntity term = findTerm(termId, companyId);
        ServiceOrderTermResponse response = ServiceOrderTermMapper.fromEntity(term);

        log.info("findById() - END - termId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public ServiceOrderTermResponse create(ServiceOrderTermCreateRequest request, Long companyId) {
        log.info("create() - START - companyId={}, title={}", companyId, request.getTitle());

        ServiceOrderTermEntity term = ServiceOrderTermMapper.toEntity(request, companyId);

        if (Boolean.TRUE.equals(term.getIsDefault())) {
            clearDefaultTerms(companyId, null);
        }

        ServiceOrderTermEntity saved = serviceOrderTermRepository.save(term);
        ServiceOrderTermResponse response = ServiceOrderTermMapper.fromEntity(saved);

        log.info("create() - END - termId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public ServiceOrderTermResponse update(Long termId, ServiceOrderTermUpdateRequest request, Long companyId) {
        log.info("update() - START - termId={}, companyId={}", termId, companyId);

        ServiceOrderTermEntity term = findTerm(termId, companyId);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultTerms(companyId, term.getId());
        }

        if (request.getIsDefault() != null) {
            term.setIsDefault(request.getIsDefault());
        }

        term.setTitle(request.getTitle());
        term.setContent(request.getContent());

        ServiceOrderTermEntity updated = serviceOrderTermRepository.save(term);
        ServiceOrderTermResponse response = ServiceOrderTermMapper.fromEntity(updated);

        log.info("update() - END - termId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public void delete(Long termId, Long companyId) {
        log.info("delete() - START - termId={}, companyId={}", termId, companyId);

        ServiceOrderTermEntity term = findTerm(termId, companyId);
        term.setStatus(ServiceOrderTermStatus.DELETED);
        term.setIsDefault(false);

        serviceOrderTermRepository.save(term);

        log.info("delete() - END - termId={}, companyId={}", termId, companyId);
    }

    @Transactional(readOnly = true)
    public void validateTermExists(Long termId, Long companyId) {
        if (termId == null) {
            return;
        }
        findTerm(termId, companyId);
    }

    private ServiceOrderTermEntity findTerm(Long termId, Long companyId) {
        return serviceOrderTermRepository.findByIdAndCompanyIdAndStatusNot(termId, companyId, ServiceOrderTermStatus.DELETED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Termo de condição não encontrado"));
    }

    private void clearDefaultTerms(Long companyId, Long excludeId) {
        List<ServiceOrderTermEntity> currentDefaults = serviceOrderTermRepository.findByCompanyIdAndIsDefaultTrue(companyId);

        for (ServiceOrderTermEntity existing : currentDefaults) {
            if (!existing.getId().equals(excludeId)) {
                existing.setIsDefault(false);
                serviceOrderTermRepository.save(existing);
            }
        }
    }
}
