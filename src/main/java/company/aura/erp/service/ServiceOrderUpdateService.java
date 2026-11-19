package company.aura.erp.service;

import company.aura.erp.dto.serviceorder.ServiceOrderUpdateCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderUpdateEditRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderUpdateResponse;
import company.aura.erp.entity.ServiceOrderUpdateEntity;
import company.aura.erp.enums.ServiceOrderUpdateEventType;
import company.aura.erp.mapper.ServiceOrderUpdateMapper;
import company.aura.erp.repository.ServiceOrderUpdateRepository;
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
public class ServiceOrderUpdateService {

    private final ServiceOrderUpdateRepository serviceOrderUpdateRepository;

    @Transactional(readOnly = true)
    public Page<ServiceOrderUpdateResponse> list(
            Long serviceOrderId,
            ServiceOrderUpdateEventType eventType,
            Long companyId,
            Pageable pageable
    ) {
        log.info("list() - START - serviceOrderId={}, companyId={}, eventType={}", serviceOrderId, companyId, eventType);

        Page<ServiceOrderUpdateResponse> page = serviceOrderUpdateRepository
                .searchByServiceOrder(companyId, serviceOrderId, eventType, pageable)
                .map(ServiceOrderUpdateMapper::fromEntity);

        log.info("list() - END - serviceOrderId={}, companyId={}, totalElements={}, totalPages={}",
                serviceOrderId, companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional
    public ServiceOrderUpdateResponse create(
            Long serviceOrderId,
            ServiceOrderUpdateCreateRequest request,
            Long companyId
    ) {
        log.info("create() - START - serviceOrderId={}, companyId={}, eventType={}",
                serviceOrderId, companyId, request.getEventType());

        ServiceOrderUpdateEntity entity = ServiceOrderUpdateMapper.toEntity(request, serviceOrderId, companyId);
        ServiceOrderUpdateEntity saved = serviceOrderUpdateRepository.save(entity);

        ServiceOrderUpdateResponse response = ServiceOrderUpdateMapper.fromEntity(saved);
        log.info("create() - END - updateId={}, serviceOrderId={}, companyId={}",
                response.getId(), serviceOrderId, companyId);
        return response;
    }

    @Transactional
    public ServiceOrderUpdateResponse update(
            Long serviceOrderId,
            Long updateId,
            ServiceOrderUpdateEditRequest request,
            Long companyId
    ) {
        log.info("update() - START - serviceOrderId={}, updateId={}, companyId={}",
                serviceOrderId, updateId, companyId);

        ServiceOrderUpdateEntity entity = findUpdate(serviceOrderId, updateId, companyId);

        entity.setEventType(request.getEventType());
        entity.setDescription(request.getDescription().trim());

        ServiceOrderUpdateEntity updated = serviceOrderUpdateRepository.save(entity);
        ServiceOrderUpdateResponse response = ServiceOrderUpdateMapper.fromEntity(updated);

        log.info("update() - END - updateId={}, serviceOrderId={}, companyId={}",
                response.getId(), serviceOrderId, companyId);
        return response;
    }

    @Transactional
    public void delete(Long serviceOrderId, Long updateId, Long companyId) {
        log.info("delete() - START - serviceOrderId={}, updateId={}, companyId={}",
                serviceOrderId, updateId, companyId);

        ServiceOrderUpdateEntity entity = findUpdate(serviceOrderId, updateId, companyId);
        serviceOrderUpdateRepository.delete(entity);

        log.info("delete() - END - updateId={}, serviceOrderId={}, companyId={}",
                updateId, serviceOrderId, companyId);
    }

    private ServiceOrderUpdateEntity findUpdate(Long serviceOrderId, Long updateId, Long companyId) {
        return serviceOrderUpdateRepository.findByIdAndServiceOrderIdAndCompanyId(updateId, serviceOrderId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Atualização da ordem de serviço não encontrada com ID: " + updateId));
    }
}
