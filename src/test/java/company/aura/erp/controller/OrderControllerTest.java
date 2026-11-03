package company.aura.erp.controller;

import company.aura.erp.dto.order.*;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.OrderService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("user@empresa.com")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("POST /api/orders — Deve abrir comanda e retornar HTTP 201")
    void shouldOpenOrder() {
        OrderCreateRequest request = new OrderCreateRequest(
                5L, "Comanda 1", Collections.emptyList()
        );
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, null, null, OrderStatus.OPEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10.00), BigDecimal.ZERO, BigDecimal.valueOf(10.00),
                "Comanda 1", null, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(orderService.openOrder(request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderResponse> response = orderController.openOrder(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).openOrder(request, 10L);
    }

    @Test
    @DisplayName("POST /api/orders — Deve abrir comanda com objeto shipping opcional e retornar HTTP 201")
    void shouldOpenOrderWithShipping() {
        OrderShippingRequest shipping = new OrderShippingRequest(
                0, 4L, "ABC1234", "SP", "12345678", 2, "CAIXA", null, null,
                BigDecimal.valueOf(4.5), BigDecimal.valueOf(4.0), null, "Coleta pela manhã", BigDecimal.valueOf(35.00)
        );
        OrderCreateRequest request = new OrderCreateRequest(
                5L, "Pedido com frete", Collections.emptyList(), shipping
        );
        OrderShippingResponse shippingResponse = new OrderShippingResponse(
                1L, 1L, 4L, 0, "ABC1234", "SP", "12345678", 2, "CAIXA", null, null,
                BigDecimal.valueOf(4.5), BigDecimal.valueOf(4.0), null, "Coleta pela manhã", LocalDateTime.now(), LocalDateTime.now()
        );
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, null, null, OrderStatus.OPEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(35.00), BigDecimal.ZERO, BigDecimal.valueOf(35.00),
                "Pedido com frete", null, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), Collections.emptyList(), shippingResponse
        );

        when(orderService.openOrder(request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderResponse> response = orderController.openOrder(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).openOrder(request, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/items — Deve adicionar item e retornar HTTP 201")
    void shouldAddItem() {
        OrderItemCreateRequest request = new OrderItemCreateRequest(
                1L, "Pomada Modeladora", BigDecimal.valueOf(2), BigDecimal.ZERO, null
        );
        OrderItemResponse responseDto = new OrderItemResponse(
                100L, 1L, 1L, CatalogItemType.PHYSICAL, "Pomada Modeladora",
                BigDecimal.valueOf(2), BigDecimal.valueOf(35.00), BigDecimal.ZERO, BigDecimal.valueOf(70.00),
                null, false, LocalDateTime.now()
        );

        when(orderService.addItem(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderItemResponse> response = orderController.addItem(1L, request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).addItem(1L, request, 10L);
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/items/{itemId} — Deve atualizar linha do item e retornar HTTP 200")
    void shouldUpdateItem() {
        OrderItemUpdateRequest request = new OrderItemUpdateRequest(
                BigDecimal.valueOf(3), BigDecimal.ZERO, 8L, "Pomada Modeladora"
        );
        OrderItemResponse responseDto = new OrderItemResponse(
                100L, 1L, 1L, CatalogItemType.PHYSICAL, "Pomada Modeladora",
                BigDecimal.valueOf(3), BigDecimal.valueOf(30.00), BigDecimal.ZERO, BigDecimal.valueOf(90.00),
                8L, false, LocalDateTime.now()
        );

        when(orderService.updateItem(1L, 100L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderItemResponse> response = orderController.updateItem(1L, 100L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).updateItem(1L, 100L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}/items/{itemId} — Deve remover item e retornar HTTP 204")
    void shouldRemoveItem() {
        ResponseEntity<Void> response = orderController.removeItem(1L, 100L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService).removeItem(1L, 100L, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve fechar comanda e retornar HTTP 200")
    void shouldCloseOrder() {
        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new OrderInstallmentRequest(1, LocalDate.now(), BigDecimal.valueOf(100.00), PaymentMethod.PIX, true, null, null, null, null, null)
        ));
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, null, null, OrderStatus.PENDING_PAYMENT,
                BigDecimal.ZERO, BigDecimal.valueOf(100.00), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100.00),
                null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(orderService.closeOrder(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderResponse> response = orderController.closeOrder(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).closeOrder(1L, request, 10L);
    }

    @Test
    @DisplayName("GET /api/orders — Deve listar pedidos de forma simplificada e paginada")
    void shouldListSummarizedOrders() {
        Pageable pageable = PageRequest.of(0, 20);
        OrderSummaryResponse summary = new OrderSummaryResponse(
                1L, "12345678901", "Cliente Teste", BigDecimal.valueOf(150.00), OrderStatus.OPEN, LocalDateTime.now()
        );
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderService.listSummarized("Teste", 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<OrderSummaryResponse>> response = orderController.list("Teste", pageable, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(orderService).listSummarized("Teste", 10L, pageable);
    }
}
