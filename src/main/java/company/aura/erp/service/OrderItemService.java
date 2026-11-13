package company.aura.erp.service;

import company.aura.erp.dto.order.OrderCreateRequest;
import company.aura.erp.dto.order.OrderItemCreateRequest;
import company.aura.erp.dto.order.OrderItemUpdateRequest;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderItemEntity;
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
                request != null && request.items() != null ? request.items().size() : 0);

        List<OrderItemEntity> items = request.items().stream()
                .map(itemReq -> createItem(itemReq, savedOrder))
                .toList();

        log.info("saveInitialItems() - END - orderId={}, savedCount={}",
                savedOrder != null ? savedOrder.getId() : null, items.size());
        return items;
    }

    public OrderItemEntity createItem(OrderItemCreateRequest itemRequest, OrderEntity order) {
        log.info("createItem() - START - orderId={}, catalogItemId={}, professionalId={}",
                order != null ? order.getId() : null,
                itemRequest != null ? itemRequest.catalogItemId() : null,
                itemRequest != null ? itemRequest.professionalId() : null);

        CatalogItemEntity catalogItem = findCatalogItem(itemRequest.catalogItemId(), order.getCompanyId());

        if (itemRequest.professionalId() != null) {
            validateCollaborator(itemRequest.professionalId(), order.getCompanyId());
        }

        BigDecimal discount = itemRequest.discountAmount() != null ? itemRequest.discountAmount() : BigDecimal.ZERO;
        BigDecimal totalPrice = itemRequest.quantity().multiply(catalogItem.getUnitPrice()).subtract(discount);

        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }

        OrderItemEntity item = OrderItemEntity.builder()
                .orderId(order.getId())
                .catalogItemId(itemRequest.catalogItemId())
                .itemType(catalogItem.getType())
                .description(itemRequest.description())
                .quantity(itemRequest.quantity())
                .unitPrice(catalogItem.getUnitPrice())
                .discountAmount(discount)
                .totalPrice(totalPrice)
                .professionalId(itemRequest.professionalId())
                .stockDeducted(false)
                .build();

        OrderItemEntity saved = orderItemRepository.save(item);
        log.info("createItem() - END - itemId={}, orderId={}, professionalId={}",
                saved.getId(), saved.getOrderId(), saved.getProfessionalId());
        return saved;
    }

    public OrderItemEntity updateOrderItem(OrderEntity order, Long itemId, OrderItemUpdateRequest request) {
        log.info("updateOrderItem() - START - orderId={}, itemId={}, professionalId={}",
                order != null ? order.getId() : null, itemId, request != null ? request.professionalId() : null);

        OrderItemEntity item = findOrderItem(order.getId(), itemId);
        CatalogItemEntity catalogItem = findCatalogItem(item.getCatalogItemId(), order.getCompanyId());

        if (request.professionalId() != null) {
            validateCollaborator(request.professionalId(), order.getCompanyId());
        }

        item.setQuantity(request.quantity());
        item.setUnitPrice(catalogItem.getUnitPrice());
        item.setDiscountAmount(request.discountAmount());
        item.setProfessionalId(request.professionalId());
        item.setDescription(request.description());

        BigDecimal discount = item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO;
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

    public void deleteItem(Long orderId, Long itemId) {
        log.info("deleteItem() - START - orderId={}, itemId={}", orderId, itemId);
        OrderItemEntity item = findOrderItem(orderId, itemId);
        orderItemRepository.delete(item);
        log.info("deleteItem() - END - orderId={}, itemId={}", orderId, itemId);
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
