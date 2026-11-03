package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.*;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderInstallmentRepository orderInstallmentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PartnerRepository partnerRepository;

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
    @DisplayName("Deve abrir comanda com sucesso com status OPEN e frete opcional")
    void shouldOpenOrderSuccessfully() {
        OrderCreateRequest request = new OrderCreateRequest(
                partnerId,
                BigDecimal.valueOf(20.00),
                "Mesa 05",
                List.of(
                        new OrderItemCreateRequest(
                                null,
                                CatalogItemType.SERVICE,
                                "Corte de Cabelo",
                                BigDecimal.valueOf(1),
                                BigDecimal.valueOf(50.00),
                                BigDecimal.ZERO,
                                5L
                        )
                )
        );

        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of(PartnerEntity.builder().id(partnerId).companyId(companyId).build()));

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity order = inv.getArgument(0);
            if (order.getId() == null) {
                order.setId(100L);
            }
            return order;
        });

        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(inv -> {
            OrderItemEntity item = inv.getArgument(0);
            item.setId(200L);
            return item;
        });

        OrderResponse response = orderService.openOrder(request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.OPEN, response.status());
        assertEquals(BigDecimal.valueOf(20.00), response.freightAmount());
        assertEquals(BigDecimal.valueOf(50.00), response.servicesAmount());
        assertEquals(BigDecimal.valueOf(70.00), response.totalAmount());
        assertEquals(1, response.items().size());
    }

    @Test
    @DisplayName("Deve adicionar item à comanda aberta e recalcular totais")
    void shouldAddItemSuccessfully() {
        OrderItemCreateRequest request = new OrderItemCreateRequest(
                null,
                CatalogItemType.PHYSICAL,
                "Shampoo Especial",
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(30.00),
                BigDecimal.valueOf(5.00),
                null
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
                .stockDeducted(false)
                .build();

        when(orderItemRepository.save(any(OrderItemEntity.class))).thenReturn(savedItem);
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(savedItem));

        OrderItemResponse response = orderService.addItem(100L, request, companyId);

        assertNotNull(response);
        assertEquals("Shampoo Especial", response.description());
        assertEquals(BigDecimal.valueOf(55.00), response.totalPrice());
        assertFalse(response.stockDeducted());

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
                null, CatalogItemType.SERVICE, "Escova",
                BigDecimal.ONE, BigDecimal.valueOf(40.00), BigDecimal.ZERO, null
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.addItem(100L, request, companyId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar quantidade e preço do item na comanda aberta")
    void shouldUpdateItemSuccessfully() {
        OrderItemEntity existingItem = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .description("Shampoo Especial")
                .quantity(BigDecimal.valueOf(1))
                .unitPrice(BigDecimal.valueOf(30.00))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(30.00))
                .stockDeducted(false)
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findById(201L)).thenReturn(Optional.of(existingItem));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(existingItem));

        OrderItemUpdateRequest request = new OrderItemUpdateRequest(
                BigDecimal.valueOf(3), BigDecimal.valueOf(25.00), BigDecimal.valueOf(5.00), 8L, null
        );

        OrderItemResponse response = orderService.updateItem(100L, 201L, request, companyId);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(3), response.quantity());
        assertEquals(BigDecimal.valueOf(25.00), response.unitPrice());
        assertEquals(BigDecimal.valueOf(70.00), response.totalPrice()); // 3 * 25 - 5 = 70
        assertEquals(8L, response.professionalId());
    }

    @Test
    @DisplayName("Deve remover item da comanda e recalcular totais")
    void shouldRemoveItemSuccessfully() {
        OrderItemEntity existingItem = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .totalPrice(BigDecimal.valueOf(50.00))
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findById(201L)).thenReturn(Optional.of(existingItem));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of());

        orderService.removeItem(100L, 201L, companyId);

        verify(orderItemRepository).delete(existingItem);
        assertEquals(BigDecimal.ZERO, sampleOrder.getProductsAmount());
        assertEquals(BigDecimal.valueOf(15.00), sampleOrder.getTotalAmount()); // apenas frete restante
    }

    @Test
    @DisplayName("Deve fechar comanda com sucesso, baixar estoque físico e registrar quitações imediatas")
    void shouldCloseOrderSuccessfully() {
        sampleOrder.setTotalAmount(BigDecimal.valueOf(100.00));

        OrderItemEntity physicalItem = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .totalPrice(BigDecimal.valueOf(100.00))
                .stockDeducted(false)
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(physicalItem));

        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new OrderInstallmentRequest(
                        1, LocalDate.now(), BigDecimal.valueOf(50.00),
                        PaymentMethod.PIX, true, null, null, null, null, null
                ),
                new OrderInstallmentRequest(
                        2, LocalDate.now().plusDays(30), BigDecimal.valueOf(50.00),
                        PaymentMethod.CREDIT_CARD, false, "VISA", "123456", null, null, null
                )
        ));

        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> {
            OrderInstallmentEntity inst = inv.getArgument(0);
            inst.setId(500L);
            return inst;
        });
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.closeOrder(100L, request, companyId);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertNotNull(response.completedAt());
        assertTrue(physicalItem.getStockDeducted(), "Estoque físico deve ser baixado");

        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransactionEntity.class));
        verify(orderInstallmentRepository, times(2)).save(any(OrderInstallmentEntity.class));
    }

    @Test
    @DisplayName("Deve falhar ao fechar comanda com soma de parcelas divergente do total")
    void shouldThrowBadRequestWhenInstallmentsTotalMismatch() {
        sampleOrder.setTotalAmount(BigDecimal.valueOf(100.00));

        OrderItemEntity item = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.SERVICE)
                .totalPrice(BigDecimal.valueOf(100.00))
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(item));

        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new OrderInstallmentRequest(
                        1, LocalDate.now(), BigDecimal.valueOf(80.00),
                        PaymentMethod.CASH, true, null, null, null, null, null
                )
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderService.closeOrder(100L, request, companyId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cancelar comanda fechada e estornar estoque físico e parcelas")
    void shouldCancelClosedOrderAndRevertStock() {
        sampleOrder.setStatus(OrderStatus.COMPLETED);

        OrderItemEntity physicalItem = OrderItemEntity.builder()
                .id(201L)
                .orderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .stockDeducted(true)
                .build();

        OrderInstallmentEntity installment = OrderInstallmentEntity.builder()
                .id(501L)
                .orderId(100L)
                .status(InstallmentStatus.PENDING)
                .build();

        when(orderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findByOrderId(100L)).thenReturn(List.of(physicalItem));
        when(orderInstallmentRepository.findByOrderId(100L)).thenReturn(List.of(installment));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.cancelOrder(100L, companyId);

        assertEquals(OrderStatus.CANCELED, response.status());
        assertFalse(physicalItem.getStockDeducted(), "Estoque físico deve ser estornado ao cancelar");
        assertEquals(InstallmentStatus.CANCELED, installment.getStatus(), "Parcelas devem ser canceladas");
    }
}
