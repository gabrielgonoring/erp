package company.aura.erp.service;

import company.aura.erp.dto.order.OrderCreateRequest;
import company.aura.erp.dto.order.OrderItemCreateRequest;
import company.aura.erp.dto.order.OrderResponse;
import company.aura.erp.dto.order.ReceivableResponse;
import company.aura.erp.dto.serviceorder.*;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.*;
import company.aura.erp.mapper.ServiceOrderItemMapper;
import company.aura.erp.mapper.ServiceOrderMapper;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.PartnerRepository;
import company.aura.erp.repository.ServiceOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final PartnerRepository partnerRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final ServiceOrderItemService serviceOrderItemService;
    private final CompanyTermService companyTermService;
    private final ServiceOrderUpdateService serviceOrderUpdateService;
    private final OrderService orderService;
    private final ReceivedService receivedService;

    @Transactional(readOnly = true)
    public Page<ServiceOrderSummaryResponse> listSummarized(
            String search,
            LocalDate startDate,
            LocalDate endDate,
            List<ServiceOrderStatus> statuses,
            Long partnerId,
            Long technicianId,
            String objectIdentifier,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listSummarized() - START - companyId={}, search={}, startDate={}, endDate={}, statuses={}, partnerId={}, technicianId={}, objectIdentifier={}",
                companyId, search, startDate, endDate, statuses, partnerId, technicianId, objectIdentifier);

        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String identifierTerm = (objectIdentifier != null && !objectIdentifier.trim().isEmpty()) ? objectIdentifier.trim() : null;

        boolean hasDateRange = (startDate != null && endDate != null);
        boolean hasStartDateOnly = (startDate != null && endDate == null);
        boolean hasEndDateOnly = (startDate == null && endDate != null);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (hasDateRange) {
            if (startDate.isAfter(endDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser posterior à data final.");
            }
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atTime(LocalTime.MAX);
        } else if (hasStartDateOnly) {
            startDateTime = startDate.atStartOfDay();
        } else if (hasEndDateOnly) {
            endDateTime = endDate.atTime(LocalTime.MAX);
        }

        boolean hasStatuses = (statuses != null && !statuses.isEmpty());
        List<ServiceOrderStatus> effectiveStatuses = hasStatuses ? statuses : List.of(ServiceOrderStatus.TRIAGE);

        Page<ServiceOrderSummaryResponse> page = serviceOrderRepository.searchSummarized(
                companyId,
                searchTerm,
                hasDateRange,
                hasStartDateOnly,
                hasEndDateOnly,
                startDateTime,
                endDateTime,
                hasStatuses,
                effectiveStatuses,
                partnerId,
                technicianId,
                identifierTerm,
                pageable
        );

        log.info("listSummarized() - END - companyId={}, totalElements={}, totalPages={}",
                companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public ServiceOrderResponse findById(Long serviceOrderId, Long companyId) {
        log.info("findById() - START - serviceOrderId={}, companyId={}", serviceOrderId, companyId);

        ServiceOrderEntity serviceOrder = findServiceOrder(serviceOrderId, companyId);
        String customerName = resolvePartnerName(serviceOrder.getPartnerId(), companyId);
        String technicianName = resolveTechnicianName(serviceOrder.getTechnicianId(), companyId);
        List<ServiceOrderItemEntity> items = serviceOrderItemService.findServiceOrderItems(serviceOrder.getId());
        List<ReceivableResponse> receivables = receivedService.findReceivableResponsesByServiceOrderId(serviceOrder.getId());

        ServiceOrderResponse response = ServiceOrderMapper.fromEntity(serviceOrder, customerName, technicianName, items, receivables);
        log.info("findById() - END - serviceOrderId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional
    public ServiceOrderResponse create(ServiceOrderCreateRequest request, Long companyId) {
        log.info("create() - START - companyId={}, partnerId={}", companyId, request.getPartnerId());

        PartnerEntity partner = findPartner(request.getPartnerId(), companyId);

        String technicianName = validateAndResolveTechnicianName(request.getTechnicianId(), companyId);

        if (request.getTermId() != null) {
            companyTermService.validateTermExists(request.getTermId(), companyId);
        }

        ServiceOrderEntity serviceOrder = ServiceOrderMapper.toEntity(request, companyId);

        ServiceOrderEntity savedServiceOrder = serviceOrderRepository.save(serviceOrder);
        String customerName = partner.getName();

        List<ServiceOrderItemEntity> savedItems = serviceOrderItemService.saveInitialItems(request, savedServiceOrder);
        recalculateTotals(savedServiceOrder, savedItems);

        savedServiceOrder = serviceOrderRepository.save(savedServiceOrder);

        ServiceOrderResponse response = ServiceOrderMapper.fromEntity(savedServiceOrder, customerName, technicianName, savedItems);
        log.info("create() - END - serviceOrderId={}, orderNumber={}, companyId={}", response.getId(), response.getOrderNumber(), companyId);
        return response;
    }

    @Transactional
    public ServiceOrderResponse update(Long serviceOrderId, ServiceOrderUpdateRequest request, Long companyId) {
        log.info("update() - START - serviceOrderId={}, companyId={}", serviceOrderId, companyId);

        ServiceOrderEntity serviceOrder = findServiceOrder(serviceOrderId, companyId);

        validateIfEditable(serviceOrder);

        if (request.getStatus() == ServiceOrderStatus.COMPLETED || request.getStatus() == ServiceOrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Para concluir ou cancelar a ordem de serviço, utilize o endpoint específico.");
        }

        PartnerEntity partnerEntity = findPartner(request.getPartnerId(), companyId);
        String technicianName = validateAndResolveTechnicianName(request.getTechnicianId(), companyId);
        if (request.getTermId() != null) {
            companyTermService.validateTermExists(request.getTermId(), companyId);
        }

        serviceOrder.setPartnerId(request.getPartnerId());
        serviceOrder.setTechnicianId(request.getTechnicianId());
        serviceOrder.setTermId(request.getTermId());
        serviceOrder.setStatus(request.getStatus());
        serviceOrder.setObjectTitle(request.getObjectTitle().trim());
        serviceOrder.setObjectIdentifier(normalizeOptionalText(request.getObjectIdentifier()));
        serviceOrder.setEntryDetails(request.getEntryDetails());
        serviceOrder.setReportedIssue(request.getReportedIssue().trim());
        serviceOrder.setTechnicalReport(request.getTechnicalReport());
        serviceOrder.setInternalNotes(request.getInternalNotes());
        serviceOrder.setEntryAt(request.getEntryAt());
        serviceOrder.setExpectedDeliveryAt(request.getExpectedDeliveryAt());

        ServiceOrderEntity saved = serviceOrderRepository.save(serviceOrder);

        List<ServiceOrderItemEntity> items = serviceOrderItemService.findServiceOrderItems(saved.getId());
        List<ReceivableResponse> receivables = receivedService.findReceivableResponsesByServiceOrderId(saved.getId());

        ServiceOrderResponse response = ServiceOrderMapper.fromEntity(saved, partnerEntity.getName(), technicianName, items, receivables);
        log.info("update() - END - serviceOrderId={}, companyId={}", response.getId(), companyId);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<ServiceOrderUpdateResponse> listUpdates(
            Long serviceOrderId,
            ServiceOrderUpdateEventType eventType,
            Long companyId,
            Pageable pageable
    ) {
        findServiceOrder(serviceOrderId, companyId);
        return serviceOrderUpdateService.list(serviceOrderId, eventType, companyId, pageable);
    }

    @Transactional
    public ServiceOrderUpdateResponse createUpdate(
            Long serviceOrderId,
            ServiceOrderUpdateCreateRequest request,
            Long companyId
    ) {
        findServiceOrder(serviceOrderId, companyId);
        return serviceOrderUpdateService.create(serviceOrderId, request, companyId);
    }

    @Transactional
    public ServiceOrderUpdateResponse updateUpdate(
            Long serviceOrderId,
            Long updateId,
            ServiceOrderUpdateEditRequest request,
            Long companyId
    ) {
        findServiceOrder(serviceOrderId, companyId);
        return serviceOrderUpdateService.update(serviceOrderId, updateId, request, companyId);
    }

    @Transactional
    public void deleteUpdate(Long serviceOrderId, Long updateId, Long companyId) {
        findServiceOrder(serviceOrderId, companyId);
        serviceOrderUpdateService.delete(serviceOrderId, updateId, companyId);
    }

    @Transactional
    public ServiceOrderItemResponse addItem(Long serviceOrderId, ServiceOrderItemCreateRequest request, Long companyId) {
        log.info("addItem() - START - serviceOrderId={}, companyId={}", serviceOrderId, companyId);

        ServiceOrderEntity serviceOrder = findServiceOrder(serviceOrderId, companyId);
        validateIfEditable(serviceOrder);

        ServiceOrderItemEntity item = serviceOrderItemService.createItem(request, serviceOrder);
        recalculateTotals(serviceOrder);

        ServiceOrderItemResponse response = ServiceOrderItemMapper.fromEntity(item);
        log.info("addItem() - END - serviceOrderId={}, itemId={}, companyId={}", serviceOrderId, response.getId(), companyId);
        return response;
    }

    @Transactional
    public ServiceOrderItemResponse updateItem(Long serviceOrderId, Long itemId, ServiceOrderItemUpdateRequest request, Long companyId) {
        log.info("updateItem() - START - serviceOrderId={}, itemId={}, companyId={}", serviceOrderId, itemId, companyId);

        ServiceOrderEntity serviceOrder = findServiceOrder(serviceOrderId, companyId);
        validateIfEditable(serviceOrder);

        ServiceOrderItemEntity item = serviceOrderItemService.updateServiceOrderItem(serviceOrder, itemId, request);
        recalculateTotals(serviceOrder);

        ServiceOrderItemResponse response = ServiceOrderItemMapper.fromEntity(item);
        log.info("updateItem() - END - serviceOrderId={}, itemId={}, companyId={}", serviceOrderId, response.getId(), companyId);
        return response;
    }

    @Transactional
    public void removeItem(Long serviceOrderId, Long itemId, Long companyId) {
        log.info("removeItem() - START - serviceOrderId={}, itemId={}, companyId={}", serviceOrderId, itemId, companyId);

        ServiceOrderEntity serviceOrder = findServiceOrder(serviceOrderId, companyId);
        validateIfEditable(serviceOrder);

        serviceOrderItemService.cancelItem(serviceOrderId, itemId);
        recalculateTotals(serviceOrder);

        log.info("removeItem() - END - serviceOrderId={}, itemId={}, companyId={}", serviceOrderId, itemId, companyId);
    }

    @Transactional
    public ServiceOrderResponse cancel(Long serviceOrderId, Long companyId) {
        log.info("cancel() - START - serviceOrderId={}, companyId={}", serviceOrderId, companyId);

        ServiceOrderEntity serviceOrder = findServiceOrder(serviceOrderId, companyId);

        if (serviceOrder.getStatus() == ServiceOrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A ordem de serviço já está cancelada.");
        }
        if (serviceOrder.getStatus() == ServiceOrderStatus.COMPLETED || serviceOrder.getOrderId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível cancelar uma ordem de serviço já concluída.");
        }

        serviceOrderItemService.cancelItems(serviceOrderId);

        serviceOrder.setStatus(ServiceOrderStatus.CANCELED);
        ServiceOrderEntity saved = serviceOrderRepository.save(serviceOrder);

        String customerName = resolvePartnerName(saved.getPartnerId(), companyId);
        String technicianName = resolveTechnicianName(saved.getTechnicianId(), companyId);
        List<ServiceOrderItemEntity> items = serviceOrderItemService.findServiceOrderItems(saved.getId());

        ServiceOrderResponse response = ServiceOrderMapper.fromEntity(saved, customerName, technicianName, items);
        log.info("cancel() - END - serviceOrderId={}, companyId={}, status={}", response.getId(), companyId, response.getStatus());
        return response;
    }

    @Transactional
    public ServiceOrderResponse complete(Long serviceOrderId, ServiceOrderCompleteRequest request, Long companyId) {
        log.info("complete() - START - serviceOrderId={}, companyId={}", serviceOrderId, companyId);

        ServiceOrderEntity serviceOrder = findServiceOrder(serviceOrderId, companyId);

        validateCompleteServiceOrder(serviceOrder);

        List<ServiceOrderItemEntity> allItems = serviceOrderItemService.findServiceOrderItems(serviceOrder.getId());

        recalculateTotals(serviceOrder, allItems);

        OrderCreateRequest orderCreateRequest = buildCreateOrderRequest(request, allItems, serviceOrder);
        OrderResponse openedOrder = orderService.openOrder(orderCreateRequest, companyId);

        serviceOrderItemService.completeItems(allItems);

        serviceOrder.setStatus(ServiceOrderStatus.COMPLETED);
        serviceOrder.setCompletedAt(LocalDateTime.now());
        serviceOrder.setOrderId(openedOrder.getId());
        ServiceOrderEntity saved = serviceOrderRepository.save(serviceOrder);

        String customerName = resolvePartnerName(saved.getPartnerId(), companyId);
        String technicianName = resolveTechnicianName(saved.getTechnicianId(), companyId);

        ServiceOrderResponse response = ServiceOrderMapper.fromEntity(saved, customerName, technicianName, allItems);
        log.info("complete() - END - serviceOrderId={}, orderId={}, companyId={}, status={}", response.getId(), openedOrder.getId(), companyId, response.getStatus());
        return response;
    }

    private OrderCreateRequest buildCreateOrderRequest(ServiceOrderCompleteRequest request, List<ServiceOrderItemEntity> allItems, ServiceOrderEntity serviceOrder) {

        var orderItems = allItems.stream().map(osItem ->
            new OrderItemCreateRequest(
                    osItem.getCatalogItemId(),
                    osItem.getDescription(),
                    osItem.getUnitPrice(),
                    osItem.getQuantity(),
                    osItem.getDiscountAmount(),
                    osItem.getProfessionalId()
            )
        ).toList();

        return new OrderCreateRequest(
                serviceOrder.getPartnerId(),
                request.getOrderNotes(),
                orderItems,
                null

        );
    }

    private void validateCompleteServiceOrder(ServiceOrderEntity serviceOrder) {
        if (serviceOrder.getStatus() == ServiceOrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível finalizar uma ordem de serviço cancelada.");
        }
        if (serviceOrder.getStatus() == ServiceOrderStatus.COMPLETED || serviceOrder.getOrderId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta ordem de serviço já foi concluída.");
        }
    }


    private PartnerEntity findPartner(Long partnerId, Long companyId) {
        return partnerRepository.findByIdAndCompanyId(partnerId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }

    private String validateAndResolveTechnicianName(Long technicianId, Long companyId) {
        if (technicianId == null) {
            return null;
        }
        CollaboratorEntity technician = collaboratorRepository
                .findByIdAndCompanyIdAndStatusNot(technicianId, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico não encontrado"));

        return technician.getName();
    }

    private String resolvePartnerName(Long partnerId, Long companyId) {
        if (partnerId == null) {
            return null;
        }
        return partnerRepository.findNameByIdAndCompanyId(partnerId, companyId)
                .orElse(null);
    }

    private String resolveTechnicianName(Long technicianId, Long companyId) {
        if (technicianId == null) {
            return null;
        }
        return collaboratorRepository.findNameByIdAndCompanyId(technicianId, companyId)
                .orElse(null);
    }

    private ServiceOrderEntity findServiceOrder(Long serviceOrderId, Long companyId) {
        return serviceOrderRepository.findByIdAndCompanyId(serviceOrderId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de serviço não encontrada com ID: " + serviceOrderId));
    }

    private void validateIfEditable(ServiceOrderEntity serviceOrder) {
        if (serviceOrder.getStatus() == ServiceOrderStatus.COMPLETED || serviceOrder.getStatus() == ServiceOrderStatus.CANCELED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A ordem de serviço não pode ser alterada pois já está finalizada ou cancelada.");
        }
    }

    private void recalculateTotals(ServiceOrderEntity serviceOrder) {
        List<ServiceOrderItemEntity> allItems = serviceOrderItemService.findServiceOrderItems(serviceOrder.getId());
        recalculateTotals(serviceOrder, allItems);
        serviceOrderRepository.save(serviceOrder);
    }

    private void recalculateTotals(ServiceOrderEntity serviceOrder, List<ServiceOrderItemEntity> items) {
        BigDecimal servicesAmount = BigDecimal.ZERO;
        BigDecimal productsAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (ServiceOrderItemEntity item : items) {
            if (item.getStatus() == ServiceOrderItemStatus.CANCELED) {
                continue;
            }
            BigDecimal itemTotal = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;

            if (item.getItemType() == CatalogItemType.SERVICE) {
                servicesAmount = servicesAmount.add(itemTotal);
            } else {
                productsAmount = productsAmount.add(itemTotal);
            }

            if (item.getDiscountAmount() != null) {
                discountAmount = discountAmount.add(item.getDiscountAmount());
            }
        }

        BigDecimal total = servicesAmount.add(productsAmount);

        serviceOrder.setServicesAmount(servicesAmount);
        serviceOrder.setProductsAmount(productsAmount);
        serviceOrder.setDiscountAmount(discountAmount);
        serviceOrder.setTotalAmount(total);
    }

    private String normalizeOptionalText(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
}
