package company.aura.erp.service;

import company.aura.erp.dto.email.CompanyEmailCreateRequest;
import company.aura.erp.dto.email.CompanyEmailResponse;
import company.aura.erp.dto.email.CompanyEmailUpdateRequest;
import company.aura.erp.entity.CompanyEmailConfigEntity;
import company.aura.erp.enums.CompanyEmailConfigStatus;
import company.aura.erp.mapper.CompanyEmailMapper;
import company.aura.erp.modules.email.config.AwsSesProperties;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.repository.CompanyEmailConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyEmailService {

    private final CompanyEmailConfigRepository companyEmailConfigRepository;
    private AwsSesProperties awsSesProperties;

    @Transactional
    public CompanyEmailResponse create(CompanyEmailCreateRequest request, Long companyId) {
        log.info("create() - START - companyId={}, notificationType={}", companyId, request != null ? request.notificationType() : null);

        validateCompanyId(companyId);

        String normalizedNotificationType = resolveAndValidateNotificationType(request.notificationType());

        if (companyEmailConfigRepository.existsByCompanyIdAndNotificationType(companyId, normalizedNotificationType)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe configuração de e-mail cadastrada para o tipo: " + normalizedNotificationType);
        }

        CompanyEmailConfigEntity entity = buildConfigEntity(request, companyId, normalizedNotificationType);
        CompanyEmailConfigEntity saved = companyEmailConfigRepository.save(entity);

        CompanyEmailResponse response = CompanyEmailMapper.fromEntity(saved);
        log.info("create() - END - id={}, companyId={}, notificationType={}", response.getId(), companyId, response.getNotificationType());
        return response;
    }

    @Transactional
    public CompanyEmailResponse update(Long id, CompanyEmailUpdateRequest request, Long companyId) {
        log.info("update() - START - id={}, companyId={}", id, companyId);

        validateCompanyId(companyId);

        CompanyEmailConfigEntity config = findConfig(id, companyId);

        String normalizedNotificationType = resolveAndValidateNotificationType(request.notificationType());

        if (companyEmailConfigRepository.existsByCompanyIdAndNotificationTypeAndIdNot(companyId, normalizedNotificationType, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe configuração de e-mail cadastrada para o tipo: " + normalizedNotificationType);
        }

        config.setNotificationType(normalizedNotificationType);
        config.setFromEmail(request.fromEmail().trim());
        config.setFromName(request.fromName().trim());
        config.setReplyTo(normalizeOptionalEmail(request.replyTo()));
        config.setStatus(CompanyEmailConfigStatus.ACTIVE);

        CompanyEmailConfigEntity updated = companyEmailConfigRepository.save(config);

        CompanyEmailResponse response = CompanyEmailMapper.fromEntity(updated);

        log.info("update() - END - id={}, companyId={}, notificationType={}", response.getId(), companyId, response.getNotificationType());
        return response;
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        log.info("delete() - START - id={}, companyId={}", id, companyId);

        validateCompanyId(companyId);

        CompanyEmailConfigEntity config = findConfig(id, companyId);
        config.setStatus(CompanyEmailConfigStatus.INACTIVE);
        companyEmailConfigRepository.save(config);

        log.info("delete() - END - id={}, companyId={}", id, companyId);
    }

    @Transactional(readOnly = true)
    public CompanyEmailResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        validateCompanyId(companyId);

        CompanyEmailConfigEntity config = findConfig(id, companyId);
        CompanyEmailResponse response = CompanyEmailMapper.fromEntity(config);

        log.info("findById() - END - id={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CompanyEmailResponse> list(
            String search,
            String notificationType,
            CompanyEmailConfigStatus status,
            Long companyId,
            Pageable pageable
    ) {
        log.info("list() - START - companyId={}, search={}, notificationType={}, status={}", companyId, search, notificationType, status);

        validateCompanyId(companyId);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String typeFilter = (notificationType != null && !notificationType.trim().isEmpty())
                ? resolveAndValidateNotificationType(notificationType)
                : null;

        Page<CompanyEmailResponse> page = companyEmailConfigRepository
                .searchByCompanyId(companyId, searchTerm, typeFilter, status, pageable)
                .map(CompanyEmailMapper::fromEntity);

        log.info("list() - END - companyId={}, totalElements={}, totalPages={}", companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }



    @Transactional(readOnly = true)
    public Optional<CompanyEmailConfigEntity> resolveActiveConfig(Long companyId, NotificationType notificationType) {
        log.info("resolveActiveConfig() - START - companyId={}, notificationType={}", companyId, notificationType);

        if (companyId == null || notificationType == null || notificationType.getInternal()) {
            return Optional.empty();
        }

        var configs =  companyEmailConfigRepository.findActiveConfigsForNotification(companyId, List.of(notificationType.name(), NotificationType.DEFAULT.name()), CompanyEmailConfigStatus.ACTIVE);

        return configs.stream()
                .filter(c -> notificationType.name().equals(c.getNotificationType()))
                .findFirst()
                .or(() -> configs.stream().findFirst());
    }

    private CompanyEmailConfigEntity buildConfigEntity(CompanyEmailCreateRequest request, Long companyId, String normalizedNotificationType) {
        return CompanyEmailConfigEntity.builder()
                .companyId(companyId)
                .notificationType(normalizedNotificationType)
                .fromEmail(StringUtils.defaultIfBlank(request.fromEmail(), awsSesProperties.getFromEmail()).trim())
                .fromName(StringUtils.defaultIfBlank(request.fromName().trim(), awsSesProperties.getFromName()))
                .replyTo(normalizeOptionalEmail(request.replyTo()))
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();
    }

    private CompanyEmailConfigEntity findConfig(Long id, Long companyId) {
        return companyEmailConfigRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração de e-mail não encontrada com ID: " + id));
    }

    private void validateCompanyId(Long companyId) {
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário não possui empresa vinculada.");
        }
    }

    private String resolveAndValidateNotificationType(String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            return NotificationType.DEFAULT.name();
        }

        String normalized = notificationType.trim().toUpperCase(Locale.ROOT);

        boolean validNotificationType = Arrays.stream(NotificationType.values())
                .anyMatch(type -> type.name().equals(normalized));

        if (!validNotificationType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de notificação inválido: " + notificationType);
        }

        return normalized;
    }

    private String normalizeOptionalEmail(String email) {
        return (email != null && !email.isBlank()) ? email.trim() : null;
    }
}
