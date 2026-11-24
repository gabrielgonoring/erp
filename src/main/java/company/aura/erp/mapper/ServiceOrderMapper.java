package company.aura.erp.mapper;

import company.aura.erp.dto.order.ReceivableResponse;
import company.aura.erp.dto.serviceorder.ServiceOrderCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderItemResponse;
import company.aura.erp.dto.serviceorder.ServiceOrderResponse;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.ServiceOrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ServiceOrderMapper {


    public static ServiceOrderEntity toEntity(ServiceOrderCreateRequest request, Long companyId){
        ServiceOrderEntity serviceOrder = ServiceOrderEntity.builder()
                .companyId(companyId)
                .partnerId(request.getPartnerId())
                .technicianId(request.getTechnicianId())
                .termId(request.getTermId())
                .status(resolveInitialStatus(request))
                .objectTitle(request.getObjectTitle().trim())
                .objectIdentifier(normalizeOptionalText(request.getObjectIdentifier()))
                .entryDetails(request.getEntryDetails())
                .reportedIssue(request.getReportedIssue().trim())
                .technicalReport(request.getTechnicalReport())
                .internalNotes(request.getInternalNotes())
                .servicesAmount(BigDecimal.ZERO)
                .productsAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .entryAt(request.getEntryAt() != null ? request.getEntryAt() : LocalDateTime.now())
                .expectedDeliveryAt(request.getExpectedDeliveryAt())
                .build();

        return serviceOrder;
    }

    private static ServiceOrderStatus resolveInitialStatus(ServiceOrderCreateRequest request) {
        var initialStatus = request.getStatus() != null ? request.getStatus() : ServiceOrderStatus.TRIAGE;

        if (ServiceOrderStatus.CANCELED.equals(initialStatus)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uma order de serviço não pode ser criada como cancelado.");
        }

        return initialStatus;
    }

    public static ServiceOrderResponse fromEntity(
            ServiceOrderEntity serviceOrder,
            String customerName,
            String technicianName,
            List<ServiceOrderItemEntity> items
    ) {
        return fromEntity(serviceOrder, customerName, technicianName, items, Collections.emptyList());
    }

    public static ServiceOrderResponse fromEntity(
            ServiceOrderEntity serviceOrder,
            String customerName,
            String technicianName,
            List<ServiceOrderItemEntity> items,
            List<ReceivableResponse> receivables
    ) {
        if (serviceOrder == null) {
            return null;
        }

        List<ServiceOrderItemResponse> itemResponses = items != null
                ? items.stream().map(ServiceOrderItemMapper::fromEntity).toList()
                : Collections.emptyList();

        return ServiceOrderResponse.builder()
                .id(serviceOrder.getId())
                .companyId(serviceOrder.getCompanyId())
                .partnerId(serviceOrder.getPartnerId())
                .customerName(customerName)
                .technicianId(serviceOrder.getTechnicianId())
                .technicianName(technicianName)
                .orderId(serviceOrder.getOrderId())
                .termId(serviceOrder.getTermId())
                .status(serviceOrder.getStatus())
                .objectTitle(serviceOrder.getObjectTitle())
                .objectIdentifier(serviceOrder.getObjectIdentifier())
                .entryDetails(serviceOrder.getEntryDetails())
                .reportedIssue(serviceOrder.getReportedIssue())
                .technicalReport(serviceOrder.getTechnicalReport())
                .internalNotes(serviceOrder.getInternalNotes())
                .servicesAmount(serviceOrder.getServicesAmount())
                .productsAmount(serviceOrder.getProductsAmount())
                .discountAmount(serviceOrder.getDiscountAmount())
                .totalAmount(serviceOrder.getTotalAmount())
                .entryAt(serviceOrder.getEntryAt())
                .expectedDeliveryAt(serviceOrder.getExpectedDeliveryAt())
                .completedAt(serviceOrder.getCompletedAt())
                .createdAt(serviceOrder.getCreatedAt())
                .updatedAt(serviceOrder.getUpdatedAt())
                .items(itemResponses)
                .receivables(receivables != null ? receivables : Collections.emptyList())
                .build();
    }

    public static ServiceOrderItemResponse fromEntity(ServiceOrderItemEntity entity) {
        return ServiceOrderItemMapper.fromEntity(entity);
    }

    private static String normalizeOptionalText(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

}
