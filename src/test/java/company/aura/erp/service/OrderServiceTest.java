package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.*;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.OrderShippingRepository;
import company.aura.erp.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private OrderPaymentService orderPaymentService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private OrderShippingRepository orderShippingRepository;

    @Mock
    private OrderShippingService orderShippingService;

    @InjectMocks
    private OrderService orderService;

    private Long companyId = 1L;
    private Long partnerId = 10L;
    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .freightAmount(BigDecimal.valueOf(15.00))
                .servicesAmount(BigDecimal.ZERO)
                .productsAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(15.00))
                .build();
    }

    @Test
    @DisplayName("POST /api/orders — Sem frete/shipping não deve criar order_shipping e frete deve ser 0.00")
    void shouldOpenOrderWithoutFreightAndNotCreateShipping() {
        OrderCreateRequest request = new OrderCreateRequest(
                partnerId,
                "Comanda balcão",
                List.of(
                        new OrderItemCreateRequest(
                                1L, "Corte de Cabelo", BigDecimal.valueOf(1), BigDecimal.ZERO, 5L
                        )
                ),
                null
        );

        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of(PartnerEntity.builder().id(partnerId).companyId(companyId).name("Cliente Teste").build()));

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(100L);
            }
            return order;
        });

        when(orderShippingService.saveInitialShipping(eq(request), any(OrderEntity.class))).thenReturn(null);

        OrderItemEntity item = OrderItemEntity.builder()
                .id(200L)
                .orderId(100L)
                .itemType(CatalogItemType.SERVICE)
                .description("Corte de Cabelo")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(50.00))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(50.00))
                .build();

        when(orderItemService.saveInitialItems(eq(request), any(OrderEntity.class))).thenReturn(List.of(item));

        OrderResponse response = orderService.openOrder(request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.OPEN, response.status());
        assertEquals(BigDecimal.ZERO, response.freightAmount());
        assertEquals(BigDecimal.valueOf(50.00), response.servicesAmount());
        assertEquals(BigDecimal.valueOf(50.00), response.totalAmount());
        assertNull(response.shipping());
    }

    @Test
    @DisplayName("POST /api/orders — Com frete/shipping deve criar order_shipping e somar frete no total")
    void shouldOpenOrderWithFreightAndCreateShipping() {
        OrderShippingRequest shippingReq = new OrderShippingRequest(
                0, 4L, "ABC1234", "SP", null, 2, "CAIXA", null, null,
                BigDecimal.valueOf(4.5), BigDecimal.valueOf(4.0), null, "Coleta manhã", BigDecimal.valueOf(35.00)
        );

        OrderCreateRequest request = new OrderCreateRequest(
                partnerId,
                "Pedido B2B com frete",
                List.of(
                        new OrderItemCreateRequest(
                                2L, "Shampoo", BigDecimal.valueOf(2), BigDecimal.ZERO, null
                        )
                ),
                shippingReq
        );

        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of(PartnerEntity.builder().id(partnerId).companyId(companyId).name("Cliente Teste").build()));

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(100L);
            }
            return order;
        });

        OrderShippingEntity savedShippingEntity = OrderShippingEntity.builder()
                .id(1L)
                .orderId(100L)
                .freightMode(0)
                .carrierId(4L)
                .packageQuantity(2)
                .packageSpecies("CAIXA")
                .grossWeightKg(BigDecimal.valueOf(4.5))
                .netWeightKg(BigDecimal.valueOf(4.0))
                .shippingNotes("Coleta manhã")
                .build();

        when(orderShippingService.saveInitialShipping(eq(request), any(OrderEntity.class))).thenReturn(savedShippingEntity);

        OrderItemEntity item = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .description("Shampoo")
                .quantity(BigDecimal.valueOf(2))
                .unitPrice(BigDecimal.valueOf(30.00))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(60.00))
                .build();

        when(orderItemService.saveInitialItems(eq(request), any(OrderEntity.class))).thenReturn(List.of(item));

        OrderResponse response = orderService.openOrder(request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.OPEN, response.status());
        assertEquals(BigDecimal.valueOf(35.00), response.freightAmount());
        assertEquals(BigDecimal.valueOf(60.00), response.productsAmount());
        assertEquals(BigDecimal.valueOf(95.00), response.totalAmount()); // 60 + 35
        assertNotNull(response.shipping());
        assertEquals(4L, response.shipping().carrierId());
    }

    @Test
    @DisplayName("Deve adicionar item à comanda aberta e recalcular totais")
    void shouldAddItemSuccessfully() {
        OrderItemCreateRequest request = new OrderItemCreateRequest(
                1L, "Shampoo Especial", BigDecimal.valueOf(2), BigDecimal.valueOf(5.00), null
        );

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderItemEntity savedItem = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .description("Shampoo Especial")
                .quantity(BigDecimal.valueOf(2))
                .unitPrice(BigDecimal.valueOf(30.00))
                .discountAmount(BigDecimal.valueOf(5.00))
                .totalPrice(BigDecimal.valueOf(55.00))
                .build();

        when(orderItemService.createItem(request, sampleOrder)).thenReturn(savedItem);
        when(orderItemService.findOrderItems(100L)).thenReturn(List.of(savedItem));
        when(orderRepository.save(sampleOrder)).thenReturn(sampleOrder);

        OrderItemResponse response = orderService.addItem(100L, request, companyId);

        assertNotNull(response);
        assertEquals("Shampoo Especial", response.description());
        assertEquals(BigDecimal.valueOf(55.00), response.totalPrice());
        assertEquals(BigDecimal.valueOf(55.00), sampleOrder.getProductsAmount());
        assertEquals(BigDecimal.valueOf(70.00), sampleOrder.getTotalAmount()); // 55 + 15 frete
        verify(orderRepository).save(sampleOrder);
    }

    @Test
    @DisplayName("Deve lançar 400 ao tentar adicionar item em pedido já fechado")
    void shouldThrowBadRequestWhenAddingItemToClosedOrder() {
        sampleOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderItemCreateRequest request = new OrderItemCreateRequest(
                1L, "Escova", BigDecimal.ONE, BigDecimal.ZERO, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.addItem(100L, request, companyId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderItemService, never()).createItem(any(), any());
    }

    @Test
    @DisplayName("Deve atualizar quantidade e preço do item na comanda aberta")
    void shouldUpdateItemSuccessfully() {
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderItemUpdateRequest request = new OrderItemUpdateRequest(
                BigDecimal.valueOf(3), BigDecimal.valueOf(5.00), 8L, "Pomada Modeladora"
        );

        OrderItemEntity updatedItem = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .description("Pomada Modeladora")
                .quantity(BigDecimal.valueOf(3))
                .unitPrice(BigDecimal.valueOf(25.00))
                .discountAmount(BigDecimal.valueOf(5.00))
                .totalPrice(BigDecimal.valueOf(70.00))
                .professionalId(8L)
                .build();

        when(orderItemService.updateOrderItem(sampleOrder, 201L, request)).thenReturn(updatedItem);
        when(orderItemService.findOrderItems(100L)).thenReturn(List.of(updatedItem));
        when(orderRepository.save(sampleOrder)).thenReturn(sampleOrder);

        OrderItemResponse response = orderService.updateItem(100L, 201L, request, companyId);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(3), response.quantity());
        assertEquals(BigDecimal.valueOf(70.00), response.totalPrice());
        assertEquals(8L, response.professionalId());
    }

    @Test
    @DisplayName("Deve remover item da comanda e recalcular totais")
    void shouldRemoveItemSuccessfully() {
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));
        when(orderItemService.findOrderItems(100L)).thenReturn(Collections.emptyList());
        when(orderRepository.save(sampleOrder)).thenReturn(sampleOrder);

        orderService.removeItem(100L, 201L, companyId);

        verify(orderItemService).deleteItem(100L, 201L);
        assertEquals(BigDecimal.ZERO, sampleOrder.getProductsAmount());
        assertEquals(BigDecimal.valueOf(15.00), sampleOrder.getTotalAmount()); // apenas frete
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve fechar comanda, fazer upsert em shipping, recalcular total_final e processar parcelas")
    void shouldCloseOrderWithShippingUpsertAndRecalculateTotal() {
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderShippingRequest shippingReq = new OrderShippingRequest(
                0, 4L, null, null, null, 2, "CAIXA", null, null,
                BigDecimal.valueOf(4.5), BigDecimal.valueOf(4.0), "TR123456789BR", null, BigDecimal.valueOf(20.00)
        );

        OrderCloseRequest request = new OrderCloseRequest(
                List.of(
                        new OrderInstallmentRequest(1, LocalDate.now(), BigDecimal.valueOf(120.00), PaymentMethod.PIX, true, null, null, null, null, null)
                ),
                shippingReq
        );

        OrderShippingEntity savedShipping = OrderShippingEntity.builder()
                .id(10L)
                .orderId(100L)
                .trackingCode("TR123456789BR")
                .build();

        when(orderShippingService.upsertShippingOnClose(100L, request)).thenReturn(savedShipping);

        OrderItemEntity item = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .totalPrice(BigDecimal.valueOf(100.00))
                .stockDeducted(true)
                .build();

        when(orderItemService.closeItem(100L)).thenReturn(List.of(item));

        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(500L)
                .orderId(100L)
                .amount(BigDecimal.valueOf(120.00))
                .build();

        when(orderPaymentService.processInstallments(eq(request), any(OrderEntity.class))).thenReturn(List.of(installment));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.closeOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING_PAYMENT, response.status());
        assertEquals(BigDecimal.valueOf(20.00), response.freightAmount());
        assertEquals(BigDecimal.valueOf(120.00), response.totalAmount()); // 100 + 20
        assertNotNull(response.shipping());
        assertEquals("TR123456789BR", response.shipping().trackingCode());
        verify(orderPaymentService).processInstallments(eq(request), any(OrderEntity.class));
    }

    @Test
    @DisplayName("GET /api/orders — Deve listar pedidos de forma simplificada e paginada no serviço")
    void shouldListSummarizedOrders() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        OrderSummaryResponse summary = new OrderSummaryResponse(
                100L, "12345678901", "Cliente Teste", BigDecimal.valueOf(200.00), OrderStatus.OPEN, LocalDateTime.now()
        );
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderRepository.searchSummarized(eq(companyId), eq("Teste"), any(Pageable.class)))
                .thenReturn(page);

        Page<OrderSummaryResponse> result = orderService.listSummarized("  Teste  ", companyId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Cliente Teste", result.getContent().get(0).customerName());
        assertEquals("12345678901", result.getContent().get(0).customerDocument());
        assertEquals(100L, result.getContent().get(0).id());
        assertEquals(BigDecimal.valueOf(200.00), result.getContent().get(0).totalAmount());
        assertEquals(OrderStatus.OPEN, result.getContent().get(0).status());
        verify(orderRepository).searchSummarized(eq(companyId), eq("Teste"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/orders/{id} — Deve retornar pedido completo com itens, frete e parcelas")
    void shouldFindOrderByIdSuccessfully() {
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .totalAmount(BigDecimal.valueOf(150.00))
                .freightAmount(BigDecimal.valueOf(20.00))
                .build();

        OrderItemEntity item = OrderItemEntity.builder()
                .id(200L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .description("Produto A")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(130.00))
                .totalPrice(BigDecimal.valueOf(130.00))
                .build();

        OrderShippingEntity shipping = OrderShippingEntity.builder()
                .id(300L)
                .orderId(100L)
                .vehiclePlate("ABC1234")
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(order));
        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of(PartnerEntity.builder().id(partnerId).companyId(companyId).name("Cliente Teste").build()));
        when(orderItemService.findOrderItems(100L)).thenReturn(List.of(item));
        when(orderPaymentService.findInstallments(100L)).thenReturn(Collections.emptyList());
        when(orderShippingService.findByOrderId(100L)).thenReturn(Optional.of(shipping));

        OrderResponse response = orderService.findById(100L, companyId);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Cliente Teste", response.customerName());
        assertEquals(1, response.items().size());
        assertNotNull(response.shipping());
        assertEquals("ABC1234", response.shipping().vehiclePlate());
        verify(orderRepository).findByIdAndCompanyId(100L, companyId);
        verify(partnerRepository).findByIdAndCompanyId(partnerId, companyId);
    }

    @Test
    @DisplayName("GET /api/orders/{id} — Deve lançar NOT_FOUND quando pedido não pertencer à empresa")
    void shouldThrowNotFoundWhenOrderNotFoundOnFindById() {
        when(orderRepository.findByIdAndCompanyId(999L, companyId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> orderService.findById(999L, companyId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("PUT /api/orders/{id} — Deve atualizar cabeçalho do pedido quando aberto")
    void shouldUpdateOrderHeaderSuccessfully() {
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .notes("Nota antiga")
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();

        Long newPartnerId = 99L;
        OrderUpdateRequest request = new OrderUpdateRequest(newPartnerId, "Nota atualizada");

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(order));
        when(partnerRepository.findByIdAndCompanyId(newPartnerId, companyId))
                .thenReturn(Optional.of(PartnerEntity.builder().id(newPartnerId).companyId(companyId).name("Novo Cliente").build()));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemService.findOrderItems(100L)).thenReturn(Collections.emptyList());
        when(orderPaymentService.findInstallments(100L)).thenReturn(Collections.emptyList());
        when(orderShippingService.findByOrderId(100L)).thenReturn(Optional.empty());

        OrderResponse response = orderService.updateOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(newPartnerId, response.partnerId());
        assertEquals("Nota atualizada", response.notes());
        assertEquals("Novo Cliente", response.customerName());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("PUT /api/orders/{id} — Deve lançar BAD_REQUEST se pedido não estiver aberto")
    void shouldThrowBadRequestWhenUpdatingNonOpenOrder() {
        OrderEntity closedOrder = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.COMPLETED)
                .build();

        OrderUpdateRequest request = new OrderUpdateRequest(partnerId, "Tentativa de alteração");

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(closedOrder));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> orderService.updateOrder(100L, request, companyId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}/shipping — Deve apagar informações de frete e zerar valor de frete quando aberto")
    void shouldDeleteShippingSuccessfully() {
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .freightAmount(BigDecimal.valueOf(30.00))
                .productsAmount(BigDecimal.valueOf(100.00))
                .totalAmount(BigDecimal.valueOf(130.00))
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(order));
        when(orderItemService.findOrderItems(100L)).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.deleteShipping(100L, companyId);

        assertEquals(BigDecimal.ZERO, order.getFreightAmount());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        verify(orderShippingService).deleteShipping(100L);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}/shipping — Deve lançar BAD_REQUEST se pedido não estiver aberto")
    void shouldThrowBadRequestWhenDeletingShippingOfNonOpenOrder() {
        OrderEntity closedOrder = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(closedOrder));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> orderService.deleteShipping(100L, companyId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderShippingService, never()).deleteShipping(any());
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/shipping — Deve alterar informações de frete e recalcular totais quando aberto")
    void shouldUpdateShippingSuccessfully() {
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .freightAmount(BigDecimal.valueOf(10.00))
                .productsAmount(BigDecimal.valueOf(100.00))
                .totalAmount(BigDecimal.valueOf(110.00))
                .build();

        OrderShippingRequest request = new OrderShippingRequest(
                1, 2L, "XYZ9999", "RJ", "98765432", 3, "PACOTE", null, null,
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(9.0), "TRACK123", "Frete rápido", BigDecimal.valueOf(40.00)
        );

        OrderShippingEntity updatedShipping = OrderShippingEntity.builder()
                .id(300L)
                .orderId(100L)
                .trackingCode("TRACK123")
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(order));
        when(orderShippingService.updateShipping(100L, request)).thenReturn(updatedShipping);
        when(orderItemService.findOrderItems(100L)).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderShippingResponse response = orderService.updateShipping(100L, request, companyId);

        assertNotNull(response);
        assertEquals("TRACK123", response.trackingCode());
        assertEquals(BigDecimal.valueOf(40.00), order.getFreightAmount());
        assertEquals(BigDecimal.valueOf(40.00), order.getTotalAmount());
        verify(orderShippingService).updateShipping(100L, request);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/shipping — Deve lançar BAD_REQUEST ao tentar alterar frete de pedido não aberto")
    void shouldThrowBadRequestWhenUpdatingShippingOfNonOpenOrder() {
        OrderEntity closedOrder = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .status(OrderStatus.CANCELED)
                .build();

        OrderShippingRequest request = new OrderShippingRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, BigDecimal.TEN
        );

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(closedOrder));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> orderService.updateShipping(100L, request, companyId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderShippingService, never()).updateShipping(any(), any());
    }
}
