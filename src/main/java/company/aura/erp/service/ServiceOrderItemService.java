package company.aura.erp.service;

import company.aura.erp.dto.serviceorder.ServiceOrderCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderItemCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderItemUpdateRequest;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.enums.ServiceOrderItemStatus;
import company.aura.erp.mapper.ServiceOrderItemMapper;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.ServiceOrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderItemService {

    private final ServiceOrderItemRepository serviceOrderItemRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final CollaboratorRepository collaboratorRepository;

    public List<ServiceOrderItemEntity> saveInitialItems(ServiceOrderCreateRequest request, ServiceOrderEntity savedServiceOrder) {
        log.info("saveInitialItems() - START - serviceOrderId={}, totalItems={}", savedServiceOrder.getId(),  request.getItems().size());

        List<ServiceOrderItemEntity> items = request.getItems().stream()
                .map(itemReq -> createItem(itemReq, savedServiceOrder))
                .toList();

        log.info("saveInitialItems() - END - serviceOrderId={}, savedCount={}", savedServiceOrder.getId(), items.size());
        return items;
    }

    public ServiceOrderItemEntity createItem(ServiceOrderItemCreateRequest itemRequest, ServiceOrderEntity serviceOrder) {
        log.info("createItem() - START - serviceOrderId={}, catalogItemId={}, professionalId={}", serviceOrder.getId() , itemRequest.getCatalogItemId(), itemRequest.getProfessionalId());

        if (itemRequest.getProfessionalId() != null) {
            validateCollaborator(itemRequest.getProfessionalId(), serviceOrder.getCompanyId());
        }

        CatalogItemEntity catalogItemEntity = findCatalogItem(itemRequest.getCatalogItemId(), serviceOrder.getCompanyId());

        ServiceOrderItemEntity itemEntity = ServiceOrderItemMapper.toEntity(itemRequest, serviceOrder, catalogItemEntity);

        ServiceOrderItemEntity saved = serviceOrderItemRepository.save(itemEntity);
        log.info("createItem() - END - itemId={}, serviceOrderId={}, status={}, professionalId={}", saved.getId(), saved.getServiceOrderId(), saved.getStatus(), saved.getProfessionalId());
        return saved;
    }

    public ServiceOrderItemEntity updateServiceOrderItem(ServiceOrderEntity serviceOrder, Long itemId, ServiceOrderItemUpdateRequest request) {
        log.info("updateServiceOrderItem() - START - serviceOrderId={}, itemId={}, professionalId={}", serviceOrder.getId(), itemId, request.getProfessionalId());

        if (request.getProfessionalId() != null) {
            validateCollaborator(request.getProfessionalId(), serviceOrder.getCompanyId());
        }
        ServiceOrderItemEntity item = findServiceOrderItem(serviceOrder.getId(), itemId);

        CatalogItemEntity catalogItem = findCatalogItem(request.getCatalogItemId(), serviceOrder.getCompanyId());
        item.setCatalogItemId(catalogItem.getId());
        item.setItemType(catalogItem.getType());
        item.setUnitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : catalogItem.getUnitPrice());
        item.setStatus(ServiceOrderItemMapper.resolveItemStatus(request.getStatus()));
        item.setProfessionalId(request.getProfessionalId());
        item.setDescription(request.getDescription());
        item.setQuantity(request.getQuantity());
        item.setDiscountAmount(request.getDiscountAmount());
        item.setStockReserved(request.getStockReserved());
        BigDecimal discount = item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO;
        item.setDiscountAmount(discount);
        item.setTotalPrice(ServiceOrderItemMapper.calculateTotalPrice(item.getQuantity(), item.getUnitPrice(), discount));

        ServiceOrderItemEntity saved = serviceOrderItemRepository.save(item);
        log.info("updateServiceOrderItem() - END - itemId={}, serviceOrderId={}, status={}, professionalId={}", saved.getId(), saved.getServiceOrderId(), saved.getStatus(), saved.getProfessionalId());
        return saved;
    }

    public void cancelItem(Long serviceOrderId, Long itemId) {
        log.info("cancelItem() - START - serviceOrderId={}, itemId={}", serviceOrderId, itemId);
        ServiceOrderItemEntity item = findServiceOrderItem(serviceOrderId, itemId);
        cancelItem(item);
        log.info("cancelItem() - END - serviceOrderId={}, itemId={}", serviceOrderId, itemId);
    }

    public void cancelItem(ServiceOrderItemEntity item) {
        log.info("cancelItem() - START - itemId={}", item.getId());

        boolean changed = false;

        if (Boolean.TRUE.equals(item.getStockReserved())) {
            item.setStockReserved(false);
            changed = true;
        }
        if (item.getStatus() != ServiceOrderItemStatus.CANCELED) {
            item.setStatus(ServiceOrderItemStatus.CANCELED);
            changed = true;
        }
        if (changed) {
            serviceOrderItemRepository.save(item);
        }

        log.info("cancelItem() - END - itemId={}", item.getId());
    }

    public void cancelItems(Long serviceOrderId) {
        log.info("cancelItems() - START - serviceOrderId={}", serviceOrderId);
        List<ServiceOrderItemEntity> items = findServiceOrderItems(serviceOrderId);
        items.forEach(this::cancelItem);
        log.info("cancelItems() - END - serviceOrderId={}", serviceOrderId);
    }

    public void completeItems(List<ServiceOrderItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ServiceOrderItemEntity item : items) {
            if (item.getStatus() != ServiceOrderItemStatus.CANCELED
                    && item.getStatus() != ServiceOrderItemStatus.COMPLETED) {
                item.setStatus(ServiceOrderItemStatus.COMPLETED);
                serviceOrderItemRepository.save(item);
            }
        }
    }

    public List<ServiceOrderItemEntity> findServiceOrderItems(Long serviceOrderId) {
        log.info("findServiceOrderItems() - START - serviceOrderId={}", serviceOrderId);
        List<ServiceOrderItemEntity> items = serviceOrderItemRepository.findByServiceOrderIdAndStatusNotIn(serviceOrderId, List.of(ServiceOrderItemStatus.CANCELED));
        log.info("findServiceOrderItems() - END - serviceOrderId={}, totalItems={}", serviceOrderId, items.size());
        return items;
    }



    private void validateCollaborator(Long professionalId, Long companyId) {
        collaboratorRepository.findByIdAndCompanyIdAndStatusNot(professionalId, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + professionalId));
    }

    private ServiceOrderItemEntity findServiceOrderItem(Long serviceOrderId, Long itemId) {
        return serviceOrderItemRepository.findById(itemId)
                .filter(i -> i.getServiceOrderId().equals(serviceOrderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado na ordem de serviço com ID: " + itemId));
    }

    private CatalogItemEntity findCatalogItem(Long catalogItemId, Long companyId) {
        return catalogItemRepository.findByIdAndCompanyId(catalogItemId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um dos itens da ordem de serviço não existe"));
    }
}
