package company.aura.erp.service;

import company.aura.erp.dto.order.OrderCreateRequest;
import company.aura.erp.dto.order.OrderItemCreateRequest;
import company.aura.erp.dto.order.OrderItemUpdateRequest;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final CatalogItemRepository catalogItemRepository;

    public List<OrderItemEntity> saveInitialItems(OrderCreateRequest request, OrderEntity savedOrder) {
        return request.items().stream()
                .map(itemReq -> createItem(itemReq, savedOrder))
                .toList();
    }

    public OrderItemEntity createItem(OrderItemCreateRequest itemRequest, OrderEntity order) {

        CatalogItemEntity catalogItem = findCatalogItem(itemRequest.catalogItemId(), order.getId());

        BigDecimal discount = itemRequest.discountAmount() != null ? itemRequest.discountAmount() : BigDecimal.ZERO;
        BigDecimal totalPrice = itemRequest.quantity().multiply(catalogItem.getUnitPrice()).subtract(discount);

        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }

        OrderItemEntity item = OrderItemEntity.builder()
                .orderId(order.getId())
                .catalogItemId(itemRequest.catalogItemId())
                .itemType(itemRequest.itemType())
                .description(itemRequest.description())
                .quantity(itemRequest.quantity())
                .unitPrice(catalogItem.getUnitPrice())
                .discountAmount(discount)
                .totalPrice(totalPrice)
                .professionalId(itemRequest.professionalId())
                .stockDeducted(false)
                .build();

        return orderItemRepository.save(item);
    }



    public OrderItemEntity updateOrderItem(Long orderId, Long itemId, OrderItemUpdateRequest request){
        OrderItemEntity item = findOrderItem(orderId, itemId);
        CatalogItemEntity catalogItem = findCatalogItem(item.getCatalogItemId(), orderId);

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

        return orderItemRepository.save(item);
    }

    public void deleteItem(Long orderId, Long itemId){
        OrderItemEntity item = findOrderItem(orderId, itemId);
        orderItemRepository.delete(item);
    }

    public List<OrderItemEntity> closeItem(Long orderId){

        List<OrderItemEntity> items = findOrderItems(orderId);

        validateItemsToClose(items);

        // 2. Baixa de estoque físico
        for (OrderItemEntity item : items) {
            if (item.getItemType() == CatalogItemType.PHYSICAL) {
                item.setStockDeducted(true);
                orderItemRepository.save(item);
            }
        }

        return items;
    }

    private void validateItemsToClose(List<OrderItemEntity> items) {
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível fechar uma comanda sem itens.");
        }
    }

    public List<OrderItemEntity> findOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    private OrderItemEntity findOrderItem(Long orderId, Long itemId) {
        return orderItemRepository.findById(itemId)
                .filter(i -> i.getOrderId().equals(orderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado no pedido com ID: " + itemId));
    }

    private CatalogItemEntity findCatalogItem(Long catalogItemId, Long orderId) {
        return catalogItemRepository.findByIdAndCompanyId(catalogItemId, orderId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um dos itens do pedido não existe"));
    }
}
