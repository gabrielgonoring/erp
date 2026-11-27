package company.aura.erp.service;

import company.aura.erp.dto.order.OrderCreateRequest;
import company.aura.erp.dto.order.OrderItemCreateRequest;
import company.aura.erp.dto.order.OrderItemUpdateRequest;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.OrderItemRepository;
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
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final CollaboratorRepository collaboratorRepository;

    public List<OrderItemEntity> saveInitialItems(OrderCreateRequest request, OrderEntity savedOrder) {
        log.info("saveInitialItems() - START - orderId={}, totalItems={}",
                savedOrder != null ? savedOrder.getId() : null,
                request != null && request.getItems() != null ? request.getItems().size() : 0);

        List<OrderItemEntity> items = request.getItems().stream()
                .map(itemReq -> createItem(itemReq, savedOrder))
                .toList();

        log.info("saveInitialItems() - END - orderId={}, savedCount={}",
                savedOrder != null ? savedOrder.getId() : null, items.size());
        return items;
    }

    public List<OrderItemEntity> saveItemsFromServiceOrder(List<ServiceOrderItemEntity> serviceOrderItems, OrderEntity savedOrder) {
        log.info("saveItemsFromServiceOrder() - START - orderId={}, totalItems={}",
                savedOrder != null ? savedOrder.getId() : null,
                serviceOrderItems != null ? serviceOrderItems.size() : 0);

        if (serviceOrderItems == null || serviceOrderItems.isEmpty()) {
            return List.of();
        }

        List<OrderItemEntity> items = serviceOrderItems.stream()
                .map(soItem -> {
                    BigDecimal discount = soItem.getDiscountAmount() != null ? soItem.getDiscountAmount() : BigDecimal.ZERO;
                    BigDecimal totalPrice = soItem.getTotalPrice();
                    if (totalPrice == null) {
                        totalPrice = soItem.getQuantity().multiply(soItem.getUnitPrice()).subtract(discount);
                        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
                            totalPrice = BigDecimal.ZERO;
                        }
                    }

                    OrderItemEntity item = OrderItemEntity.builder()
                            .orderId(savedOrder.getId())
                            .catalogItemId(soItem.getCatalogItemId())
                            .itemType(soItem.getItemType())
                            .description(soItem.getDescription())
                            .quantity(soItem.getQuantity())
                            .unitPrice(soItem.getUnitPrice())
                            .discountAmount(discount)
                            .totalPrice(totalPrice)
                            .professionalId(soItem.getProfessionalId())
                            .stockDeducted(false)
                            .build();
                    return orderItemRepository.save(item);
                })
                .toList();

        log.info("saveItemsFromServiceOrder() - END - orderId={}, savedCount={}",
                savedOrder != null ? savedOrder.getId() : null, items.size());
        return items;
    }

    public OrderItemEntity createItem(OrderItemCreateRequest itemRequest, OrderEntity order) {
        log.info("createItem() - START - orderId={}, catalogItemId={}, professionalId={}",
                order != null ? order.getId() : null,
                itemRequest != null ? itemRequest.getCatalogItemId() : null,
                itemRequest != null ? itemRequest.getProfessionalId() : null);

        CatalogItemEntity catalogItem = findCatalogItem(itemRequest.getCatalogItemId(), order.getCompanyId());

        if (itemRequest.getProfessionalId() != null) {
            validateCollaborator(itemRequest.getProfessionalId(), order.getCompanyId());
        }

        BigDecimal unitPrice = itemRequest.getUnitPrice() != null ? itemRequest.getUnitPrice() : catalogItem.getUnitPrice();
        BigDecimal discount = itemRequest.getDiscountAmount() != null ? itemRequest.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalPrice = itemRequest.getQuantity().multiply(unitPrice).subtract(discount);

        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }

        OrderItemEntity item = OrderItemEntity.builder()
                .orderId(order.getId())
                .catalogItemId(itemRequest.getCatalogItemId())
                .itemType(catalogItem.getType())
                .description(itemRequest.getDescription())
                .quantity(itemRequest.getQuantity())
                .unitPrice(unitPrice)
                .discountAmount(discount)
                .totalPrice(totalPrice)
                .professionalId(itemRequest.getProfessionalId())
                .stockDeducted(false)
                .build();

        OrderItemEntity saved = orderItemRepository.save(item);
        log.info("createItem() - END - itemId={}, orderId={}, professionalId={}",
                saved.getId(), saved.getOrderId(), saved.getProfessionalId());
        return saved;
    }

    public OrderItemEntity updateOrderItem(OrderEntity order, Long itemId, OrderItemUpdateRequest request) {
        log.info("updateOrderItem() - START - orderId={}, itemId={}, professionalId={}", order.getId(), itemId, request.getProfessionalId());

        OrderItemEntity item = findOrderItem(order.getId(), itemId);
        CatalogItemEntity catalogItem = findCatalogItem(getCatalogItemId(request, item), order.getCompanyId());

        if (request.getProfessionalId() != null) {
            validateCollaborator(request.getProfessionalId(), order.getCompanyId());
        }

        BigDecimal resolvedUnitPrice = resolveUnitPriceOnUpdate(request, item, catalogItem);

        item.setCatalogItemId(catalogItem.getId());
        item.setItemType(catalogItem.getType());
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        item.setUnitPrice(resolvedUnitPrice);
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        item.setDiscountAmount(discount);
        item.setProfessionalId(request.getProfessionalId());
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }

        BigDecimal totalPrice = item.getQuantity().multiply(item.getUnitPrice()).subtract(discount);

        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }

        item.setTotalPrice(totalPrice);

        OrderItemEntity saved = orderItemRepository.save(item);
        log.info("updateOrderItem() - END - itemId={}, orderId={}, professionalId={}",
                saved.getId(), saved.getOrderId(), saved.getProfessionalId());
        return saved;
    }

    private BigDecimal resolveUnitPriceOnUpdate(
            OrderItemUpdateRequest request,
            OrderItemEntity item,
            CatalogItemEntity catalogItem
    ) {
        if (request.getUnitPrice() != null) {
            return request.getUnitPrice();
        }
        if (request.getCatalogItemId() != null && !request.getCatalogItemId().equals(item.getCatalogItemId())) {
            return catalogItem.getUnitPrice();
        }
        return item.getUnitPrice() != null ? item.getUnitPrice() : catalogItem.getUnitPrice();
    }

    private Long getCatalogItemId(OrderItemUpdateRequest request, OrderItemEntity item) {
        return request.getCatalogItemId() != null ? request.getCatalogItemId() : item.getCatalogItemId();
    }

    public void deleteItem(Long orderId, Long itemId) {
        log.info("deleteItem() - START - orderId={}, itemId={}", orderId, itemId);
        OrderItemEntity item = findOrderItem(orderId, itemId);
        orderItemRepository.delete(item);
        log.info("deleteItem() - END - orderId={}, itemId={}", orderId, itemId);
    }

    public void deleteAllByOrderId(Long orderId) {
        log.info("deleteAllByOrderId() - START - orderId={}", orderId);
        orderItemRepository.deleteByOrderId(orderId);
        log.info("deleteAllByOrderId() - END - orderId={}", orderId);
    }

    public List<OrderItemEntity> cancelItems(Long orderId) {
        log.info("cancelItems() - START - orderId={}", orderId);
        List<OrderItemEntity> items = findOrderItems(orderId);
        for (OrderItemEntity item : items) {
            if (Boolean.TRUE.equals(item.getStockDeducted())) {
                item.setStockDeducted(false);
                orderItemRepository.save(item);
            }
        }
        log.info("cancelItems() - END - orderId={}, itemsCount={}", orderId, items.size());
        return items;
    }

    public List<OrderItemEntity> closeItem(Long orderId) {
        log.info("closeItem() - START - orderId={}", orderId);
        List<OrderItemEntity> items = findOrderItems(orderId);

        validateItemsToClose(items);

        // Baixa de estoque físico
        for (OrderItemEntity item : items) {
            if (item.getItemType() == CatalogItemType.PHYSICAL) {
                item.setStockDeducted(true);
                orderItemRepository.save(item);
            }
        }

        log.info("closeItem() - END - orderId={}, closedCount={}", orderId, items.size());
        return items;
    }

    private void validateCollaborator(Long professionalId, Long companyId) {
        log.info("validateCollaborator() - START - professionalId={}, companyId={}", professionalId, companyId);

        collaboratorRepository.findByIdAndCompanyIdAndStatusNot(professionalId, companyId, CollaboratorStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado com ID: " + professionalId));

        log.info("validateCollaborator() - END - professionalId={}", professionalId);
    }

    private void validateItemsToClose(List<OrderItemEntity> items) {
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível fechar uma comanda sem itens.");
        }
    }

    public List<OrderItemEntity> findOrderItems(Long orderId) {
        log.info("findOrderItems() - START - orderId={}", orderId);
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(orderId);
        log.info("findOrderItems() - END - orderId={}, totalItems={}", orderId, items.size());
        return items;
    }

    private OrderItemEntity findOrderItem(Long orderId, Long itemId) {
        return orderItemRepository.findById(itemId)
                .filter(i -> i.getOrderId().equals(orderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado no pedido com ID: " + itemId));
    }

    private CatalogItemEntity findCatalogItem(Long catalogItemId, Long companyId) {
        return catalogItemRepository.findByIdAndCompanyId(catalogItemId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um dos itens do pedido não existe"));
    }
}
