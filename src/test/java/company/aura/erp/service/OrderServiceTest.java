package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.InvoicePurpose;
import company.aura.erp.security.UserPrincipal;
import org.springframework.mock.web.MockMultipartFile;

import company.aura.erp.entity.*;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableOriginType;
import company.aura.erp.enums.ReceivableStatus;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private ReceivedService receivedService;

    @Mock
    private OrderItemService orderItemService;

    @Mock
    private OrderShippingRepository orderShippingRepository;

    @Mock
    private OrderShippingService orderShippingService;

    @Mock
    private OrderInvoiceService orderInvoiceService;

    @Mock
    private CommissionService commissionService;

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
                                1L, "Corte de Cabelo", null, BigDecimal.valueOf(1), BigDecimal.ZERO, 5L
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
        assertEquals(OrderStatus.OPEN, response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getFreightAmount());
        assertEquals(BigDecimal.valueOf(50.00), response.getServicesAmount());
        assertEquals(BigDecimal.valueOf(50.00), response.getTotalAmount());
        assertNull(response.getShipping());
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
                                2L, "Shampoo", null, BigDecimal.valueOf(2), BigDecimal.ZERO, null
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
        assertEquals(OrderStatus.OPEN, response.getStatus());
        assertEquals(BigDecimal.valueOf(35.00), response.getFreightAmount());
        assertEquals(BigDecimal.valueOf(60.00), response.getProductsAmount());
        assertEquals(BigDecimal.valueOf(95.00), response.getTotalAmount()); // 60 + 35
        assertNotNull(response.getShipping());
        assertEquals(4L, response.getShipping().getCarrierId());
    }

    @Test
    @DisplayName("Deve adicionar item à comanda aberta e recalcular totais")
    void shouldAddItemSuccessfully() {
        OrderItemCreateRequest request = new OrderItemCreateRequest(
                1L, "Shampoo Especial", null, BigDecimal.valueOf(2), BigDecimal.valueOf(5.00), null
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
        assertEquals("Shampoo Especial", response.getDescription());
        assertEquals(BigDecimal.valueOf(55.00), response.getTotalPrice());
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
                1L, "Escova", null, BigDecimal.ONE, BigDecimal.ZERO, null
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

        OrderItemUpdateRequest request = new OrderItemUpdateRequest(null,
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
        assertEquals(BigDecimal.valueOf(3), response.getQuantity());
        assertEquals(BigDecimal.valueOf(70.00), response.getTotalPrice());
        assertEquals(8L, response.getProfessionalId());
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
    @DisplayName("POST /api/orders/{id}/close — Deve fechar comanda com status COMPLETED quando todos os recebíveis forem pagos")
    void shouldCloseOrderWithShippingUpsertAndRecalculateTotal() {
        sampleOrder.setTotalAmount(BigDecimal.valueOf(120.00));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderShippingRequest shippingReq = new OrderShippingRequest(
                0, 4L, null, null, null, 2, "CAIXA", null, null,
                BigDecimal.valueOf(4.5), BigDecimal.valueOf(4.0), "TR123456789BR", null, BigDecimal.valueOf(20.00)
        );

        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(List.of(
                        new ReceivableRequest(100L, null, ReceivableOriginType.ORDER, partnerId, 1, LocalDate.now(), BigDecimal.valueOf(120.00), PaymentMethod.PIX, true, null, null, null, null, null, null)
                ))
                .shipping(shippingReq)
                .build();

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

        ReceivableResponse receivableResponse = ReceivableResponse.builder()
                .id(500L)
                .orderId(100L)
                .status(ReceivableStatus.PAID)
                .amount(BigDecimal.valueOf(120.00))
                .build();

        when(receivedService.findReceivableResponses(100L))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(receivableResponse));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.closeOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.getStatus());
        assertEquals(BigDecimal.valueOf(20.00), response.getFreightAmount());
        assertEquals(BigDecimal.valueOf(120.00), response.getTotalAmount()); // 100 + 20
        assertNotNull(response.getShipping());
        assertEquals("TR123456789BR", response.getShipping().getTrackingCode());
        assertEquals(1, response.getReceivables().size());
        verify(receivedService).processReceivables(request.getReceivables(), companyId);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve fechar comanda com status COMPLETED quando houver recebível pendente")
    void shouldCloseOrderWithCompletedStatusWhenReceivablesPending() {
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(List.of(
                        new ReceivableRequest(100L, null, ReceivableOriginType.ORDER, partnerId, 1, LocalDate.now(), BigDecimal.valueOf(15.00), PaymentMethod.BOLETO, false, null, null, null, null, null, null)
                ))
                .build();

        when(orderShippingService.upsertShippingOnClose(100L, request)).thenReturn(null);

        OrderItemEntity item = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .totalPrice(BigDecimal.valueOf(15.00))
                .stockDeducted(true)
                .build();

        when(orderItemService.closeItem(100L)).thenReturn(List.of(item));

        ReceivableResponse receivableResponse = ReceivableResponse.builder()
                .id(500L)
                .orderId(100L)
                .status(ReceivableStatus.PENDING)
                .amount(BigDecimal.valueOf(15.00))
                .build();

        when(receivedService.findReceivableResponses(100L))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(receivableResponse));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.closeOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.getStatus());
        verify(receivedService).processReceivables(request.getReceivables(), companyId);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve fechar comanda com status COMPLETED quando recebíveis forem mistos")
    void shouldCloseOrderWithCompletedStatusWhenMixedReceivables() {
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(List.of(
                        new ReceivableRequest(100L, null, ReceivableOriginType.ORDER, partnerId, 1, LocalDate.now(), BigDecimal.valueOf(10.00), PaymentMethod.CASH, true, null, null, null, null, null, null),
                        new ReceivableRequest(100L, null, ReceivableOriginType.ORDER, partnerId, 2, LocalDate.now().plusMonths(1), BigDecimal.valueOf(5.00), PaymentMethod.BOLETO, false, null, null, null, null, null, null)
                ))
                .build();

        when(orderShippingService.upsertShippingOnClose(100L, request)).thenReturn(null);

        OrderItemEntity item = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .totalPrice(BigDecimal.valueOf(15.00))
                .stockDeducted(true)
                .build();

        when(orderItemService.closeItem(100L)).thenReturn(List.of(item));

        ReceivableResponse rec1 = ReceivableResponse.builder()
                .id(501L)
                .orderId(100L)
                .status(ReceivableStatus.PAID)
                .amount(BigDecimal.valueOf(10.00))
                .build();
        ReceivableResponse rec2 = ReceivableResponse.builder()
                .id(502L)
                .orderId(100L)
                .status(ReceivableStatus.PENDING)
                .amount(BigDecimal.valueOf(5.00))
                .build();

        when(receivedService.findReceivableResponses(100L))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(rec1, rec2));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.closeOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.getStatus());
        verify(receivedService).processReceivables(request.getReceivables(), companyId);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve lançar HTTP 422 quando soma dos recebíveis divergir do total do pedido")
    void shouldThrow422WhenReceivablesTotalMismatchOnCloseOrder() {
        sampleOrder.setTotalAmount(BigDecimal.valueOf(135.00));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(List.of(
                        new ReceivableRequest(100L, null, ReceivableOriginType.ORDER, partnerId, 1, LocalDate.now(), BigDecimal.valueOf(100.00), PaymentMethod.PIX, true, null, null, null, null, null, null)
                ))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.closeOrder(100L, request, companyId)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("100.00"));
        assertTrue(ex.getReason().contains("135.00"));
        verify(receivedService).findReceivableResponses(100L);
        verify(receivedService, never()).processReceivables(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve somar recebíveis já vinculados ao pedido (ignorando cancelados) com os da request")
    void shouldCloseOrderConsideringAlreadyLinkedReceivables() {
        sampleOrder.setFreightAmount(BigDecimal.ZERO);
        sampleOrder.setTotalAmount(BigDecimal.valueOf(150.00));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        ReceivableResponse existingLinked = ReceivableResponse.builder()
                .id(400L)
                .orderId(100L)
                .serviceOrderId(50L)
                .originType(ReceivableOriginType.SERVICE_ORDER)
                .status(ReceivableStatus.PAID)
                .amount(BigDecimal.valueOf(50.00))
                .build();
        ReceivableResponse existingCanceled = ReceivableResponse.builder()
                .id(401L)
                .orderId(100L)
                .serviceOrderId(50L)
                .originType(ReceivableOriginType.SERVICE_ORDER)
                .status(ReceivableStatus.CANCELED)
                .amount(BigDecimal.valueOf(99.00))
                .build();
        ReceivableResponse newReceivable = ReceivableResponse.builder()
                .id(500L)
                .orderId(100L)
                .originType(ReceivableOriginType.ORDER)
                .status(ReceivableStatus.PENDING)
                .amount(BigDecimal.valueOf(100.00))
                .build();

        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(List.of(
                        new ReceivableRequest(null, null, null, null, 1, LocalDate.now(), BigDecimal.valueOf(100.00), PaymentMethod.PIX, false, null, null, null, null, null, null)
                ))
                .build();

        OrderItemEntity item = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.SERVICE)
                .totalPrice(BigDecimal.valueOf(150.00))
                .stockDeducted(false)
                .build();

        when(orderShippingService.upsertShippingOnClose(100L, request)).thenReturn(null);
        when(orderItemService.closeItem(100L)).thenReturn(List.of(item));
        when(receivedService.findReceivableResponses(100L))
                .thenReturn(List.of(existingLinked, existingCanceled))
                .thenReturn(List.of(existingLinked, existingCanceled, newReceivable));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.closeOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.getStatus());
        assertEquals(3, response.getReceivables().size());
        verify(receivedService).processReceivables(request.getReceivables(), companyId);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve fechar pedido sem novas parcelas quando recebíveis já vinculados cobrem o total")
    void shouldCloseOrderWhenAlreadyLinkedReceivablesCoverTotal() {
        sampleOrder.setFreightAmount(BigDecimal.ZERO);
        sampleOrder.setTotalAmount(BigDecimal.valueOf(150.00));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        ReceivableResponse existingLinked = ReceivableResponse.builder()
                .id(400L)
                .orderId(100L)
                .serviceOrderId(50L)
                .originType(ReceivableOriginType.SERVICE_ORDER)
                .status(ReceivableStatus.PAID)
                .amount(BigDecimal.valueOf(150.00))
                .build();

        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(Collections.emptyList())
                .build();

        OrderItemEntity item = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.SERVICE)
                .totalPrice(BigDecimal.valueOf(150.00))
                .stockDeducted(false)
                .build();

        when(orderShippingService.upsertShippingOnClose(100L, request)).thenReturn(null);
        when(orderItemService.closeItem(100L)).thenReturn(List.of(item));
        when(receivedService.findReceivableResponses(100L)).thenReturn(List.of(existingLinked));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.closeOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.getStatus());
        assertEquals(1, response.getReceivables().size());
        verify(receivedService, never()).processReceivables(any(), any());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve lançar HTTP 422 quando soma dos recebíveis vinculados e enviados ultrapassar o total")
    void shouldThrow422WhenAlreadyLinkedPlusRequestedReceivablesExceedTotal() {
        sampleOrder.setTotalAmount(BigDecimal.valueOf(150.00));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        ReceivableResponse existingLinked = ReceivableResponse.builder()
                .id(400L)
                .orderId(100L)
                .serviceOrderId(50L)
                .originType(ReceivableOriginType.SERVICE_ORDER)
                .status(ReceivableStatus.PAID)
                .amount(BigDecimal.valueOf(50.00))
                .build();

        when(receivedService.findReceivableResponses(100L)).thenReturn(List.of(existingLinked));

        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(List.of(
                        new ReceivableRequest(null, null, null, null, 1, LocalDate.now(), BigDecimal.valueOf(150.00), PaymentMethod.PIX, true, null, null, null, null, null, null)
                ))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.closeOrder(100L, request, companyId)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("200.00"));
        assertTrue(ex.getReason().contains("150.00"));
        verify(receivedService, never()).processReceivables(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve lançar HTTP 422 quando soma dos recebíveis vinculados e enviados for menor que o total")
    void shouldThrow422WhenAlreadyLinkedPlusRequestedReceivablesAreLessThanTotal() {
        sampleOrder.setTotalAmount(BigDecimal.valueOf(150.00));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        ReceivableResponse existingLinked = ReceivableResponse.builder()
                .id(400L)
                .orderId(100L)
                .serviceOrderId(50L)
                .originType(ReceivableOriginType.SERVICE_ORDER)
                .status(ReceivableStatus.PENDING)
                .amount(BigDecimal.valueOf(50.00))
                .build();

        when(receivedService.findReceivableResponses(100L)).thenReturn(List.of(existingLinked));

        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(List.of(
                        new ReceivableRequest(null, null, null, null, 1, LocalDate.now(), BigDecimal.valueOf(80.00), PaymentMethod.PIX, true, null, null, null, null, null, null)
                ))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.closeOrder(100L, request, companyId)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("130.00"));
        assertTrue(ex.getReason().contains("150.00"));
        verify(receivedService, never()).processReceivables(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("GET /api/orders — Deve listar pedidos de forma simplificada e paginada no serviço")
    void shouldListSummarizedOrders() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        OrderSummaryResponse summary = new OrderSummaryResponse(
                100L, "12345678901", "Cliente Teste", BigDecimal.valueOf(200.00), OrderStatus.OPEN, LocalDateTime.now()
        );
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderRepository.searchSummarized(
                eq(companyId), eq("Teste"), eq(false), eq(false), eq(false), isNull(), isNull(), eq(false), anyList(), any(Pageable.class)
        )).thenReturn(page);

        Page<OrderSummaryResponse> result = orderService.listSummarized("Teste", companyId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Cliente Teste", result.getContent().get(0).getCustomerName());
        assertEquals("12345678901", result.getContent().get(0).getCustomerDocument());
        assertEquals(100L, result.getContent().get(0).getId());
        assertEquals(BigDecimal.valueOf(200.00), result.getContent().get(0).getTotalAmount());
        assertEquals(OrderStatus.OPEN, result.getContent().get(0).getStatus());
        verify(orderRepository).searchSummarized(
                eq(companyId), eq("Teste"), eq(false), eq(false), eq(false), isNull(), isNull(), eq(false), anyList(), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/orders — Deve listar pedidos com filtros de startDate, endDate e status no serviço")
    void shouldListSummarizedOrdersWithFilters() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<OrderStatus> statuses = List.of(OrderStatus.OPEN, OrderStatus.COMPLETED);

        OrderSummaryResponse summary = new OrderSummaryResponse(
                100L, "12345678901", "Cliente Teste", BigDecimal.valueOf(200.00), OrderStatus.OPEN, LocalDateTime.now()
        );
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderRepository.searchSummarized(
                eq(companyId), eq("Teste"), eq(true), eq(false), eq(false), any(LocalDateTime.class), any(LocalDateTime.class), eq(true), eq(statuses), any(Pageable.class)
        )).thenReturn(page);

        Page<OrderSummaryResponse> result = orderService.listSummarized("Teste", startDate, endDate, statuses, companyId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(orderRepository).searchSummarized(
                eq(companyId), eq("Teste"), eq(true), eq(false), eq(false), any(LocalDateTime.class), any(LocalDateTime.class), eq(true), eq(statuses), any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/orders — Deve lançar BAD_REQUEST se startDate for posterior a endDate")
    void shouldThrowBadRequestWhenStartDateIsAfterEndDate() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.of(2026, 11, 30);
        LocalDate endDate = LocalDate.of(2026, 11, 1);

        assertThrows(ResponseStatusException.class, () ->
                orderService.listSummarized("Teste", startDate, endDate, null, companyId, pageable)
        );
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
        when(orderShippingService.findByOrderId(100L)).thenReturn(Optional.empty());

        OrderResponse response = orderService.updateOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(newPartnerId, response.getPartnerId());
        assertEquals("Nota atualizada", response.getNotes());
        assertEquals("Novo Cliente", response.getCustomerName());
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
                .status(OrderStatus.COMPLETED)
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
        assertEquals("TRACK123", response.getTrackingCode());
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

    @Test
    @DisplayName("linkExternalInvoice - Deve vincular nota fiscal NFE ao pedido com sucesso")
    void shouldLinkExternalInvoiceNfeSuccess() {
        UserPrincipal user = UserPrincipal.builder().id(1L).companyId(companyId).build();
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .totalAmount(BigDecimal.valueOf(250.00))
                .freightAmount(BigDecimal.valueOf(20.00))
                .productsAmount(BigDecimal.valueOf(230.00))
                .notes("Obs do pedido")
                .build();

        MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "application/xml", "<xml/>".getBytes());
        MockMultipartFile pdfFile = new MockMultipartFile("pdfFile", "danfe.pdf", "application/pdf", "%PDF".getBytes());
        OrderExternalInvoiceRequest request = OrderExternalInvoiceRequest.builder()
                .documentType(DocumentType.NFE)
                .operationType(1)
                .externalReference("accessKey123")
                .totalAmount(BigDecimal.valueOf(250.00))
                .build();

        InvoiceResponse invoiceResponse = InvoiceResponse.builder()
                .id(555L)
                .companyId(companyId)
                .partnerId(partnerId)
                .documentType(DocumentType.NFE)
                .status("ISSUED")
                .totalAmount(BigDecimal.valueOf(250.00))
                .freightAmount(BigDecimal.valueOf(20.00))
                .productsAmount(BigDecimal.valueOf(230.00))
                .additionalInformation("Obs do pedido")
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(order));
        when(orderInvoiceService.linkExternalInvoice(order, request, xmlFile, pdfFile, user))
                .thenReturn(invoiceResponse);

        InvoiceResponse response = orderService.linkExternalInvoice(100L, request, xmlFile, pdfFile, user);

        assertNotNull(response);
        assertEquals(555L, response.getId());
        verify(orderInvoiceService).linkExternalInvoice(order, request, xmlFile, pdfFile, user);
    }

    @Test
    @DisplayName("linkExternalInvoice - Deve vincular nota fiscal NFSE ao pedido com sucesso")
    void shouldLinkExternalInvoiceNfseSuccess() {
        UserPrincipal user = UserPrincipal.builder().id(1L).companyId(companyId).build();
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();

        MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfse.xml", "application/xml", "<xml/>".getBytes());
        MockMultipartFile pdfFile = new MockMultipartFile("pdfFile", "nfse.pdf", "application/pdf", "%PDF".getBytes());
        OrderExternalInvoiceRequest request = OrderExternalInvoiceRequest.builder()
                .documentType(DocumentType.NFSE)
                .operationType(1)
                .externalReference("accessKey123")
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();

        InvoiceResponse invoiceResponse = InvoiceResponse.builder()
                .id(777L)
                .companyId(companyId)
                .partnerId(partnerId)
                .documentType(DocumentType.NFSE)
                .status("ISSUED")
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(order));
        when(orderInvoiceService.linkExternalInvoice(order, request, xmlFile, pdfFile, user))
                .thenReturn(invoiceResponse);

        InvoiceResponse response = orderService.linkExternalInvoice(100L, request, xmlFile, pdfFile, user);

        assertNotNull(response);
        assertEquals(777L, response.getId());
        verify(orderInvoiceService).linkExternalInvoice(order, request, xmlFile, pdfFile, user);
    }

    @Test
    @DisplayName("linkExternalInvoice - Deve lancar CONFLICT se nota ja vinculada com o mesmo proposito")
    void shouldThrowConflictWhenInvoiceAlreadyLinkedWithSamePurpose() {
        UserPrincipal user = UserPrincipal.builder().id(1L).companyId(companyId).build();
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();

        OrderExternalInvoiceRequest request = OrderExternalInvoiceRequest.builder()
                .documentType(DocumentType.NFE)
                .operationType(1)
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(order));
        when(orderInvoiceService.linkExternalInvoice(order, request, null, null, user))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Esta nota fiscal já está vinculada a este pedido com a mesma finalidade."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.linkExternalInvoice(100L, request, null, null, user)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(orderInvoiceService).linkExternalInvoice(order, request, null, null, user);
    }

    @Test
    @DisplayName("linkExternalInvoice - Deve lancar BAD_REQUEST se pedido cancelado")
    void shouldThrowBadRequestWhenLinkingToCanceledOrder() {
        UserPrincipal user = UserPrincipal.builder().id(1L).companyId(companyId).build();
        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.CANCELED)
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();

        OrderExternalInvoiceRequest request = OrderExternalInvoiceRequest.builder()
                .documentType(DocumentType.NFE)
                .operationType(1)
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.linkExternalInvoice(100L, request, null, null, user)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(orderInvoiceService);
    }

    @Test
    @DisplayName("linkInvoice - Deve vincular nota fiscal existente ao pedido com sucesso")
    void shouldLinkExistingInvoiceSuccess() {
        UserPrincipal user = UserPrincipal.builder().id(1L).companyId(companyId).build();
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));
        LinkOrderInvoice linkRequest = new LinkOrderInvoice(InvoicePurpose.PRODUCT, "Obs");
        OrderInvoiceResponse expectedResponse = OrderInvoiceResponse.builder()
                .id(1L)
                .companyId(companyId)
                .orderId(100L)
                .invoiceId(200L)
                .invoicePurpose(InvoicePurpose.PRODUCT)
                .notes("Obs")
                .build();

        when(orderInvoiceService.linkInvoice(200L, linkRequest, user, sampleOrder)).thenReturn(expectedResponse);

        OrderInvoiceResponse response = orderService.linkInvoice(100L, 200L, linkRequest, user);

        assertNotNull(response);
        assertEquals(200L, response.getInvoiceId());
        assertEquals(InvoicePurpose.PRODUCT, response.getInvoicePurpose());
        verify(orderInvoiceService).linkInvoice(200L, linkRequest, user, sampleOrder);
    }

    @Test
    @DisplayName("findInvoicesByOrderId - Deve retornar lista de notas do pedido")
    void shouldFindInvoicesByOrderId() {
        when(orderRepository.existsByIdAndCompanyId(100L, companyId)).thenReturn(true);
        OrderInvoiceResponse expectedResponse = OrderInvoiceResponse.builder()
                .id(1L)
                .companyId(companyId)
                .orderId(100L)
                .invoiceId(200L)
                .invoicePurpose(InvoicePurpose.PRODUCT)
                .build();
        when(orderInvoiceService.findInvoicesByOrderId(companyId, 100L)).thenReturn(List.of(expectedResponse));

        List<OrderInvoiceResponse> list = orderService.findInvoicesByOrderId(100L, companyId);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(200L, list.get(0).getInvoiceId());
        verify(orderInvoiceService).findInvoicesByOrderId(companyId, 100L);
    }

    @Test
    @DisplayName("unlinkInvoice - Deve desvincular nota fiscal com sucesso")
    void shouldUnlinkInvoiceSuccess() {
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        orderService.unlinkInvoice(100L, 200L, companyId);

        verify(orderInvoiceService).unlinkInvoice(100L, 200L, companyId);
    }

    @Test
    @DisplayName("unlinkInvoice - Deve lancar NOT_FOUND se pedido nao existir")
    void shouldThrowNotFoundWhenOrderDoesNotExistOnUnlink() {
        when(orderRepository.findByIdAndCompanyId(999L, companyId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.unlinkInvoice(999L, 200L, companyId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(orderInvoiceService);
    }

    @Test
    @DisplayName("unlinkInvoice - Deve lancar BAD_REQUEST se pedido cancelado")
    void shouldThrowBadRequestWhenUnlinkingFromCanceledOrder() {
        sampleOrder.setStatus(OrderStatus.CANCELED);
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.unlinkInvoice(100L, 200L, companyId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(orderInvoiceService);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve aplicar desconto adicional no fechamento somando aos descontos dos itens e abatendo do total")
    void shouldCloseOrderWithAdditionalDiscount() {
        sampleOrder.setFreightAmount(BigDecimal.ZERO);
        sampleOrder.setDiscountAmount(new BigDecimal("15.00"));
        sampleOrder.setTotalAmount(new BigDecimal("135.00"));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderCloseRequest request = OrderCloseRequest.builder()
                .discountAmount(new BigDecimal("20.00"))
                .receivables(List.of(
                        new ReceivableRequest(100L, null, ReceivableOriginType.ORDER, partnerId, 1, LocalDate.now(), new BigDecimal("115.00"), PaymentMethod.PIX, true, null, null, null, null, null, null)
                ))
                .build();

        OrderItemEntity item1 = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .discountAmount(new BigDecimal("10.00"))
                .totalPrice(new BigDecimal("90.00"))
                .stockDeducted(true)
                .build();
        OrderItemEntity item2 = OrderItemEntity.builder()
                .id(202L)
                .orderId(100L)
                .itemType(CatalogItemType.SERVICE)
                .discountAmount(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("45.00"))
                .stockDeducted(false)
                .build();

        when(orderShippingService.upsertShippingOnClose(100L, request)).thenReturn(null);
        when(orderItemService.closeItem(100L)).thenReturn(List.of(item1, item2));
        when(receivedService.findReceivableResponses(100L))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(ReceivableResponse.builder().id(500L).orderId(100L).status(ReceivableStatus.PAID).amount(new BigDecimal("115.00")).build()));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.closeOrder(100L, request, companyId);

        assertEquals(OrderStatus.COMPLETED, response.getStatus());
        assertEquals(new BigDecimal("35.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("115.00"), response.getTotalAmount());
        verify(receivedService).processReceivables(request.getReceivables(), companyId);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve lançar HTTP 422 quando soma dos recebíveis não considerar desconto adicional do pedido")
    void shouldThrow422WhenReceivablesDoNotMatchTotalMinusOrderDiscount() {
        sampleOrder.setFreightAmount(BigDecimal.ZERO);
        sampleOrder.setTotalAmount(new BigDecimal("135.00"));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderCloseRequest request = OrderCloseRequest.builder()
                .discountAmount(new BigDecimal("20.00"))
                .receivables(List.of(
                        new ReceivableRequest(100L, null, ReceivableOriginType.ORDER, partnerId, 1, LocalDate.now(), new BigDecimal("135.00"), PaymentMethod.PIX, true, null, null, null, null, null, null)
                ))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.closeOrder(100L, request, companyId)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("135.00"));
        assertTrue(ex.getReason().contains("115.00"));
        verify(receivedService, never()).processReceivables(any(), any());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve lançar HTTP 422 quando desconto adicional for maior que o total do pedido")
    void shouldThrow422WhenAdditionalDiscountExceedsOrderTotal() {
        sampleOrder.setTotalAmount(new BigDecimal("100.00"));
        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));

        OrderCloseRequest request = OrderCloseRequest.builder()
                .discountAmount(new BigDecimal("120.00"))
                .receivables(Collections.emptyList())
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.closeOrder(100L, request, companyId)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(receivedService, never()).processReceivables(any(), any());
    }
}
