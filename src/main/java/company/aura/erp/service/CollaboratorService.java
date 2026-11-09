package company.aura.erp.service;

import company.aura.erp.dto.collaborator.CollaboratorCreateRequest;
import company.aura.erp.dto.collaborator.CollaboratorResponse;
import company.aura.erp.dto.collaborator.CollaboratorSummaryResponse;
import company.aura.erp.dto.collaborator.CollaboratorUpdateRequest;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.enums.UserStatus;
import company.aura.erp.mapper.CollaboratorMapper;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.UserRepository;
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
public class CollaboratorService {

    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    @Transactional
    public CollaboratorResponse create(CollaboratorCreateRequest request, Long companyId) {
        log.info("create() - START - companyId={}, name={}, email={}, userId={}",
                companyId,
                request != null ? request.getName() : null,
                request != null ? request.getEmail() : null,
                request != null ? request.getUserId() : null);

        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário logado não possui uma empresa vinculada.");
        }

        validateUser(request.getUserId(), companyId);

        CollaboratorEntity collaborator = toEntity(request, companyId);
        CollaboratorEntity saved = collaboratorRepository.save(collaborator);
        CollaboratorResponse response = toResponse(saved);

        log.info("create() = END - collaboratorId={}, name={}, companyId={}",
                response.getId(), response.getName(), companyId);
        return response;
    }

    @Transactional
    public CollaboratorResponse update(Long id, CollaboratorUpdateRequest request, Long companyId) {
        log.info("update() - START - id={}, companyId={}, name={}",
                id, companyId, request != null ? request.getName() : null);

        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + id));

        if (request.getUserId() != null) {
            validateUser(request.getUserId(), companyId);
        }

        collaborator.setName(request.getName());
        collaborator.setUserId(request.getUserId());
        collaborator.setCpf(request.getCpf());
        collaborator.setEmail(request.getEmail());
        collaborator.setPhone(request.getPhone());

        collaborator.setStreet(request.getStreet());
        collaborator.setNumber(request.getNumber());
        collaborator.setComplement(request.getComplement());
        collaborator.setNeighborhood(request.getNeighborhood());
        collaborator.setCity(request.getCity());
        collaborator.setState(request.getState());
        collaborator.setZipCode(request.getZipCode());
        collaborator.setIbgeCityCode(request.getIbgeCityCode());

        collaborator.setRg(request.getRg());
        collaborator.setBirthDate(request.getBirthDate());
        collaborator.setAdmissionDate(request.getAdmissionDate());
        collaborator.setResignationDate(request.getResignationDate());
        collaborator.setJobTitle(request.getJobTitle());
        collaborator.setPisPasep(request.getPisPasep());

        CollaboratorEntity updated = collaboratorRepository.save(collaborator);
        CollaboratorResponse response = toResponse(updated);

        log.info("update() = END - id={}, name={}, companyId={}",
                response.getId(), response.getName(), companyId);
        return response;
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        log.info("delete() - START - id={}, companyId={}", id, companyId);

        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + id));

        collaborator.setStatus(CollaboratorStatus.DELETED.name());
        collaboratorRepository.save(collaborator);

        log.info("delete() = END - id={}, status={}", id, collaborator.getStatus());
    }


    public Page<CollaboratorSummaryResponse> listPaginated(String search, String status, Long companyId, Pageable pageable) {
        log.info("listPaginated() - START - companyId={}, search={}, status={}, pageable={}", companyId, search, status, pageable);

        Page<CollaboratorSummaryResponse> page = collaboratorRepository.searchByCompanyIdAndTerm(companyId, search, status, pageable)
                .map(this::toSummaryResponse);

        log.info("listPaginated() = END - companyId={}, totalElements={}, totalPages={}",
                companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public CollaboratorResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        CollaboratorEntity collaborator = collaboratorRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + id));

        CollaboratorResponse response = toResponse(collaborator);

        log.info("findById() = END - id={}, collaborator: {}", id, response);
        return response;
    }

    public CollaboratorEntity toEntity(CollaboratorCreateRequest request, Long companyId) {
        return CollaboratorMapper.toEntity(request, companyId);
    }

    public CollaboratorResponse toResponse(CollaboratorEntity entity) {
        return CollaboratorMapper.toResponse(entity);
    }

    public CollaboratorSummaryResponse toSummaryResponse(CollaboratorEntity entity) {
        return CollaboratorMapper.toSummaryResponse(entity);
    }

    private void validateUser(Long userId, Long companyId) {
        log.info("validateUser() - START - userId={}, companyId={}", userId, companyId);

        if (userId != null) {
            userRepository.findByIdAndCompanyId(userId, companyId)
                    .filter(u -> !UserStatus.DELETED.name().equalsIgnoreCase(u.getStatus()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com ID: " + userId));
        }

        log.info("validateUser() = END - userId={}", userId);
    }
}
