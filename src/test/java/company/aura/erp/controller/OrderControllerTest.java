package company.aura.erp.controller;

import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.order.*;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.*;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.OrderService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                .email("admin@empresa.com")
                .name("Admin")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("POST /api/orders — Deve abrir comanda sem frete e retornar HTTP 201")
    void shouldOpenOrder() {
        OrderCreateRequest request = new OrderCreateRequest(5L, "Comanda mesa 2", Collections.emptyList());
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, OrderStatus.OPEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "Comanda mesa 2", null, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), Collections.emptyList()
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
                1L, 10L, 5L, OrderStatus.OPEN,
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
    @DisplayName("GET /api/orders/{id} — Deve retornar pedido por ID")
    void shouldFindOrderById() {
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, "Cliente Teste", OrderStatus.OPEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "Comanda mesa 2", null, LocalDateTime.now(), LocalDateTime.now(),
                Collections.emptyList(), Collections.emptyList()
        );

        when(orderService.findById(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderResponse> response = orderController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        assertEquals("Cliente Teste", response.getBody().getCustomerName());
        verify(orderService).findById(1L, 10L);
    }

    @Test
    @DisplayName("PUT /api/orders/{id} — Deve atualizar dados do cabeçalho do pedido")
    void shouldUpdateOrderHeader() {
        OrderUpdateRequest request = new OrderUpdateRequest(8L, "Observação atualizada");
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 8L, OrderStatus.OPEN,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "Observação atualizada", null, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(orderService.updateOrder(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderResponse> response = orderController.updateOrder(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).updateOrder(1L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id} — Deve apagar pedido em aberto e retornar HTTP 204")
    void shouldDeleteOpenOrder() {
        ResponseEntity<Void> response = orderController.deleteOrder(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService).deleteOrder(1L, 10L);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/shipping — Deve alterar as informações de frete do pedido")
    void shouldUpdateOrderShipping() {
        OrderShippingRequest request = new OrderShippingRequest(
                1, 2L, "XYZ9999", "RJ", "98765432", 3, "PACOTE", null, null,
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(9.0), "TRACK123", "Frete rápido", BigDecimal.valueOf(50.00)
        );
        OrderShippingResponse shippingResponse = new OrderShippingResponse(
                1L, 1L, 2L, 1, "XYZ9999", "RJ", "98765432", 3, "PACOTE", null, null,
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(9.0), "TRACK123", "Frete rápido", LocalDateTime.now(), LocalDateTime.now()
        );

        when(orderService.updateShipping(1L, request, 10L)).thenReturn(shippingResponse);

        ResponseEntity<OrderShippingResponse> response = orderController.updateShipping(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(shippingResponse, response.getBody());
        verify(orderService).updateShipping(1L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}/shipping — Deve apagar as informações de frete do pedido")
    void shouldDeleteOrderShipping() {
        ResponseEntity<Void> response = orderController.deleteShipping(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService).deleteShipping(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/items — Deve adicionar item à comanda e retornar HTTP 201")
    void shouldAddItemToOrder() {
        OrderItemCreateRequest request = new OrderItemCreateRequest(
                2L, "Refrigerante Cola", new BigDecimal("9.50"), BigDecimal.valueOf(2), BigDecimal.ZERO, null
        );
        OrderItemResponse itemResponse = new OrderItemResponse(
                100L, 1L, 2L, CatalogItemType.PHYSICAL, "Refrigerante Cola",
                BigDecimal.valueOf(2), new BigDecimal("9.50"), BigDecimal.ZERO, new BigDecimal("19.00"),
                null, false, LocalDateTime.now()
        );

        when(orderService.addItem(1L, request, 10L)).thenReturn(itemResponse);

        ResponseEntity<OrderItemResponse> response = orderController.addItem(1L, request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(itemResponse, response.getBody());
        verify(orderService).addItem(1L, request, 10L);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/items/{itemId} — Deve atualizar item da comanda incluindo unitPrice e retornar HTTP 200")
    void shouldUpdateItemInOrder() {
        OrderItemUpdateRequest request = OrderItemUpdateRequest.builder()
                .quantity(BigDecimal.valueOf(3))
                .unitPrice(new BigDecimal("12.00"))
                .discountAmount(BigDecimal.valueOf(2.00))
                .description("Refrigerante com preço editado")
                .build();
        OrderItemResponse itemResponse = new OrderItemResponse(
                100L, 1L, 2L, CatalogItemType.PHYSICAL, "Refrigerante com preço editado",
                BigDecimal.valueOf(3), new BigDecimal("12.00"), BigDecimal.valueOf(2.00), new BigDecimal("34.00"),
                null, false, LocalDateTime.now()
        );

        when(orderService.updateItem(1L, 100L, request, 10L)).thenReturn(itemResponse);

        ResponseEntity<OrderItemResponse> response = orderController.updateItem(1L, 100L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(itemResponse, response.getBody());
        verify(orderService).updateItem(1L, 100L, request, 10L);
    }

    @Test
    @DisplayName("OrderItemCreateRequest e OrderItemUpdateRequest — Deve falhar na validação quando unitPrice for negativo")
    void shouldFailValidationWhenItemUnitPriceIsNegative() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        OrderItemCreateRequest createRequest = OrderItemCreateRequest.builder()
                .catalogItemId(1L)
                .description("Item teste")
                .unitPrice(new BigDecimal("-1.00"))
                .quantity(BigDecimal.ONE)
                .build();
        Set<ConstraintViolation<OrderItemCreateRequest>> createViolations = validator.validate(createRequest);
        assertFalse(createViolations.isEmpty());
        assertEquals("O preço unitário não pode ser negativo.", createViolations.iterator().next().getMessage());

        OrderItemUpdateRequest updateRequest = OrderItemUpdateRequest.builder()
                .unitPrice(new BigDecimal("-0.01"))
                .build();
        Set<ConstraintViolation<OrderItemUpdateRequest>> updateViolations = validator.validate(updateRequest);
        assertFalse(updateViolations.isEmpty());
        assertEquals("O preço unitário não pode ser negativo.", updateViolations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}/items/{itemId} — Deve remover item da comanda e retornar HTTP 204")
    void shouldRemoveItemFromOrder() {
        ResponseEntity<Void> response = orderController.removeItem(1L, 100L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService).removeItem(1L, 100L, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close/check-similar — Deve retornar verificação de pedidos similares e HTTP 200")
    void shouldCheckSimilarOrdersOnClose() {
        OrderCloseRequest request = OrderCloseRequest.builder()
                .discountAmount(BigDecimal.valueOf(10.00))
                .completedAt(LocalDateTime.of(2026, 11, 20, 14, 30))
                .build();
        OrderSummaryResponse similar = new OrderSummaryResponse(
                2L, "12345678901", "Cliente Teste", BigDecimal.valueOf(90.00), OrderStatus.COMPLETED, LocalDateTime.of(2026, 11, 20, 10, 0)
        );
        OrderSimilarCheckResponse responseDto = OrderSimilarCheckResponse.builder()
                .hasSimilar(true)
                .similarOrders(List.of(similar))
                .build();

        when(orderService.checkSimilarOnClose(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderSimilarCheckResponse> response = orderController.checkSimilarOnClose(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        assertTrue(response.getBody().isHasSimilar());
        assertEquals(1, response.getBody().getSimilarOrders().size());
        verify(orderService).checkSimilarOnClose(1L, request, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve fechar comanda e retornar HTTP 200")
    void shouldCloseOrder() {
        OrderCloseRequest request = OrderCloseRequest.builder()
                .receivables(List.of(
                        new ReceivableRequest(1L, null, ReceivableOriginType.ORDER, 5L, 1, LocalDate.now(), BigDecimal.valueOf(100.00), PaymentMethod.PIX, true, null, null, null, null, null, null)
                ))
                .build();
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, OrderStatus.COMPLETED,
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
    @DisplayName("POST /api/orders/{id}/close — Deve fechar comanda com desconto adicional e retornar HTTP 200")
    void shouldCloseOrderWithDiscountAmount() {
        OrderCloseRequest request = OrderCloseRequest.builder()
                .discountAmount(BigDecimal.valueOf(10.00))
                .receivables(List.of(
                        new ReceivableRequest(1L, null, ReceivableOriginType.ORDER, 5L, 1, LocalDate.now(), BigDecimal.valueOf(90.00), PaymentMethod.PIX, true, null, null, null, null, null, null)
                ))
                .build();
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, OrderStatus.COMPLETED,
                BigDecimal.ZERO, BigDecimal.valueOf(100.00), BigDecimal.ZERO, BigDecimal.valueOf(10.00), BigDecimal.valueOf(90.00),
                null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(orderService.closeOrder(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderResponse> response = orderController.closeOrder(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).closeOrder(1L, request, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve repassar completedAt opcional no fechamento")
    void shouldCloseOrderWithCompletedAt() {
        LocalDateTime customDate = LocalDateTime.of(2026, 11, 15, 16, 45);
        OrderCloseRequest request = OrderCloseRequest.builder()
                .completedAt(customDate)
                .build();
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, OrderStatus.COMPLETED,
                BigDecimal.ZERO, BigDecimal.valueOf(100.00), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100.00),
                null, customDate, customDate, LocalDateTime.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(orderService.closeOrder(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderResponse> response = orderController.closeOrder(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(customDate, response.getBody().getCompletedAt());
        assertEquals(customDate, response.getBody().getCreatedAt());
        verify(orderService).closeOrder(1L, request, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel — Deve cancelar pedido fechado e retornar HTTP 200")
    void shouldCancelClosedOrder() {
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, OrderStatus.CANCELED,
                BigDecimal.ZERO, BigDecimal.valueOf(100.00), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100.00),
                null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(orderService.cancelOrder(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<OrderResponse> response = orderController.cancelOrder(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        assertEquals(OrderStatus.CANCELED, response.getBody().getStatus());
        verify(orderService).cancelOrder(1L, 10L);
    }


    @Test
    @DisplayName("OrderCloseRequest — Deve falhar na validação por anotação quando discountAmount for negativo")
    void shouldFailValidationWhenDiscountAmountIsNegative() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        OrderCloseRequest request = OrderCloseRequest.builder()
                .discountAmount(new BigDecimal("-1.00"))
                .build();

        Set<ConstraintViolation<OrderCloseRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals("O valor de desconto não pode ser negativo.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/invoice/external — Deve vincular nota fiscal externa ao pedido e retornar HTTP 201")
    void shouldLinkExternalInvoice() {
        Long orderId = 1L;
        DocumentType documentType = DocumentType.NFE;
        MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "application/xml", "<xml/>".getBytes());
        MockMultipartFile pdfFile = new MockMultipartFile("pdfFile", "danfe.pdf", "application/pdf", "%PDF".getBytes());
        OrderExternalInvoiceRequest formRequest = OrderExternalInvoiceRequest.builder()
                .documentType(documentType)
                .operationType(1)
                .build();

        InvoiceEntity invoiceEntity = InvoiceEntity.builder()
                .id(100L)
                .companyId(10L)
                .partnerId(5L)
                .documentType(DocumentType.NFE)
                .status("ISSUED")
                .totalAmount(BigDecimal.valueOf(150.00))
                .build();
        InvoiceResponse invoiceResponse = company.aura.erp.mapper.InvoiceMapper.fromEntity(invoiceEntity);

        when(orderService.linkExternalInvoice(orderId, formRequest, xmlFile, pdfFile, userPrincipal))
                .thenReturn(invoiceResponse);

        ResponseEntity<InvoiceResponse> response = orderController.linkExternalInvoice(
                orderId, xmlFile, pdfFile, formRequest, userPrincipal
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(invoiceResponse, response.getBody());
        verify(orderService).linkExternalInvoice(orderId, formRequest, xmlFile, pdfFile, userPrincipal);
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/invoices — Deve listar notas vinculadas ao pedido")
    void shouldGetOrderInvoices() {
        Long orderId = 1L;
        OrderInvoiceResponse invoiceDto = new OrderInvoiceResponse(
                10L, 10L, orderId, 100L, InvoicePurpose.PRODUCT, "Nota vinculada", LocalDateTime.now()
        );

        when(orderService.findInvoicesByOrderId(orderId, 10L)).thenReturn(List.of(invoiceDto));

        ResponseEntity<List<OrderInvoiceResponse>> response = orderController.getOrderInvoices(orderId, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(100L, response.getBody().get(0).getInvoiceId());
        verify(orderService).findInvoicesByOrderId(orderId, 10L);
    }

    @Test
    @DisplayName("DELETE /api/orders/{orderId}/invoice/{invoiceId} — Deve desvincular nota fiscal do pedido e retornar HTTP 204")
    void shouldUnlinkInvoice() {
        Long orderId = 1L;
        Long invoiceId = 100L;

        ResponseEntity<Void> response = orderController.unlinkInvoice(orderId, invoiceId, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService).unlinkInvoice(orderId, invoiceId, 10L);
    }

    @Test
    @DisplayName("GET /api/orders — Deve listar pedidos de forma simplificada e paginada sem filtros opcionais")
    void shouldListSummarizedOrders() {
        Pageable pageable = PageRequest.of(0, 20);
        OrderSummaryResponse summary = new OrderSummaryResponse(
                1L, "12345678901", "Cliente Teste", BigDecimal.valueOf(150.00), OrderStatus.OPEN, LocalDateTime.now()
        );
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderService.listSummarized("Teste", null, null, null, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<OrderSummaryResponse>> response = orderController.list("Teste", null, null, null, pageable, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(orderService).listSummarized("Teste", null, null, null, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/orders — Deve listar pedidos filtrando por startDate, endDate e status")
    void shouldListSummarizedOrdersWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<OrderStatus> statuses = List.of(OrderStatus.OPEN, OrderStatus.COMPLETED);

        OrderSummaryResponse summary = new OrderSummaryResponse(
                1L, "12345678901", "Cliente Teste", BigDecimal.valueOf(150.00), OrderStatus.OPEN, LocalDateTime.now()
        );
        Page<OrderSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderService.listSummarized("Teste", startDate, endDate, statuses, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<OrderSummaryResponse>> response = orderController.list("Teste", startDate, endDate, statuses, pageable, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(orderService).listSummarized("Teste", startDate, endDate, statuses, 10L, pageable);
    }
}
