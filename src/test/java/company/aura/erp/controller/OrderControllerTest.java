package company.aura.erp.controller;

import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.order.*;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.*;
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
import org.springframework.mock.web.MockMultipartFile;

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
                2L, "Refrigerante Cola", null, BigDecimal.valueOf(2), BigDecimal.ZERO, null
        );
        OrderItemResponse itemResponse = new OrderItemResponse(
                100L, 1L, 2L, CatalogItemType.PHYSICAL, "Refrigerante Cola",
                BigDecimal.valueOf(2), BigDecimal.valueOf(8.00), BigDecimal.ZERO, BigDecimal.valueOf(16.00),
                null, false, LocalDateTime.now()
        );

        when(orderService.addItem(1L, request, 10L)).thenReturn(itemResponse);

        ResponseEntity<OrderItemResponse> response = orderController.addItem(1L, request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(itemResponse, response.getBody());
        verify(orderService).addItem(1L, request, 10L);
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/items/{itemId} — Deve atualizar item da comanda e retornar HTTP 200")
    void shouldUpdateItemInOrder() {
        OrderItemUpdateRequest request = new OrderItemUpdateRequest(null,
                BigDecimal.valueOf(3), BigDecimal.valueOf(2.00), null, "Refrigerante com desconto"
        );
        OrderItemResponse itemResponse = new OrderItemResponse(
                100L, 1L, 2L, CatalogItemType.PHYSICAL, "Refrigerante com desconto",
                BigDecimal.valueOf(3), BigDecimal.valueOf(8.00), BigDecimal.valueOf(2.00), BigDecimal.valueOf(22.00),
                null, false, LocalDateTime.now()
        );

        when(orderService.updateItem(1L, 100L, request, 10L)).thenReturn(itemResponse);

        ResponseEntity<OrderItemResponse> response = orderController.updateItem(1L, 100L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(itemResponse, response.getBody());
        verify(orderService).updateItem(1L, 100L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}/items/{itemId} — Deve remover item da comanda e retornar HTTP 204")
    void shouldRemoveItemFromOrder() {
        ResponseEntity<Void> response = orderController.removeItem(1L, 100L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService).removeItem(1L, 100L, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/receivables — Deve adicionar recebível ao pedido e retornar HTTP 201")
    void shouldAddReceivable() {
        ReceivableCreateRequest request = new ReceivableCreateRequest(
                1, LocalDate.now(), BigDecimal.valueOf(50.00), PaymentMethod.PIX, false, null, null, null, null, null, null
        );
        ReceivableResponse responseDto = new ReceivableResponse(
                1L, 1L, 10L, 5L, 1, LocalDate.now(), BigDecimal.valueOf(50.00),
                PaymentMethod.PIX, ReceivableStatus.PENDING, null, null, null, null, null, null,
                BigDecimal.ZERO, BigDecimal.valueOf(50.00), Collections.emptyList()
        );

        when(orderService.addReceivable(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<ReceivableResponse> response = orderController.addReceivable(1L, request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).addReceivable(1L, request, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/receivables/{receivableId}/pay — Deve registrar pagamento do recebível")
    void shouldPayReceivable() {
        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(25.00), PaymentMethod.PIX, null, null, null, "Parcial"
        );
        ReceivableResponse responseDto = new ReceivableResponse(
                1L, 1L, 10L, 5L, 1, LocalDate.now(), BigDecimal.valueOf(50.00),
                PaymentMethod.PIX, ReceivableStatus.PARTIALLY_PAID, null, null, null, null, null, null,
                BigDecimal.valueOf(25.00), BigDecimal.valueOf(25.00), Collections.emptyList()
        );

        when(orderService.payReceivable(1L, 1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<ReceivableResponse> response = orderController.payReceivable(1L, 1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).payReceivable(1L, 1L, request, 10L);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/receivables/{receivableId} — Deve registrar pagamento via PUT")
    void shouldUpdateReceivablePayment() {
        PaymentTransactionRequest request = new PaymentTransactionRequest(
                BigDecimal.valueOf(50.00), PaymentMethod.PIX, null, null, null, "Quitação"
        );
        ReceivableResponse responseDto = new ReceivableResponse(
                1L, 1L, 10L, 5L, 1, LocalDate.now(), BigDecimal.valueOf(50.00),
                PaymentMethod.PIX, ReceivableStatus.PAID, null, null, null, null, null, null,
                BigDecimal.valueOf(50.00), BigDecimal.ZERO, Collections.emptyList()
        );

        when(orderService.payReceivable(1L, 1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<ReceivableResponse> response = orderController.updateReceivable(1L, 1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).payReceivable(1L, 1L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}/receivables/{receivableId} — Deve cancelar recebível e retornar HTTP 204")
    void shouldCancelReceivable() {
        ResponseEntity<Void> response = orderController.cancelReceivable(1L, 1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService).cancelReceivable(1L, 1L, 10L);
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/receivables/{receivableId}/cancel — Deve cancelar recebível e retornar HTTP 200")
    void shouldCancelReceivablePatch() {
        ReceivableResponse responseDto = new ReceivableResponse(
                1L, 1L, 10L, 5L, 1, LocalDate.now(), BigDecimal.valueOf(50.00),
                PaymentMethod.PIX, ReceivableStatus.CANCELED, null, null, null, null, null, null,
                BigDecimal.ZERO, BigDecimal.valueOf(50.00), Collections.emptyList()
        );

        when(orderService.cancelReceivable(1L, 1L, 10L)).thenReturn(responseDto);

        ResponseEntity<ReceivableResponse> response = orderController.cancelReceivablePatch(1L, 1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(orderService).cancelReceivable(1L, 1L, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/receivables/{receivableId}/payment-file — Deve fazer upload do comprovante/boleto e retornar HTTP 200")
    void shouldUploadPaymentFile() {
        MockMultipartFile file = new MockMultipartFile("file", "boleto.pdf", "application/pdf", "content".getBytes());
        ReceivableResponse receivableResponse = new ReceivableResponse();
        receivableResponse.setId(2L);
        receivableResponse.setPaymentFileUrl("http://storage/boleto.pdf");

        when(orderService.uploadPaymentFile(1L, 2L, file, "12345", 10L)).thenReturn(receivableResponse);

        ResponseEntity<ReceivableResponse> response = orderController.uploadPaymentFile(1L, 2L, file, "12345", userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(receivableResponse, response.getBody());
        verify(orderService).uploadPaymentFile(1L, 2L, file, "12345", 10L);
    }

    @Test
    @DisplayName("GET /api/orders/{id}/receivables/{receivableId}/payment-file — Deve retornar URL temporária do arquivo de pagamento")
    void shouldGetPaymentFile() {
        when(orderService.getPaymentFileUrl(1L, 2L, 10L)).thenReturn("http://storage/presigned-url");

        ResponseEntity<PaymentFileUrlResponse> response = orderController.getPaymentFile(1L, 2L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new PaymentFileUrlResponse("http://storage/presigned-url"), response.getBody());
        verify(orderService).getPaymentFileUrl(1L, 2L, 10L);
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}/receivables/{receivableId}/payment-file — Deve apagar o arquivo de pagamento do recebível e retornar HTTP 204")
    void shouldDeletePaymentFile() {
        ResponseEntity<Void> response = orderController.deletePaymentFile(1L, 2L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService).deletePaymentFile(1L, 2L, 10L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/close — Deve fechar comanda e retornar HTTP 200")
    void shouldCloseOrder() {
        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new ReceivableRequest(1, LocalDate.now(), BigDecimal.valueOf(100.00), PaymentMethod.PIX, true, null, null, null, null, null, null)
        ));
        OrderResponse responseDto = new OrderResponse(
                1L, 10L, 5L, OrderStatus.PENDING_PAYMENT,
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
        List<OrderStatus> statuses = List.of(OrderStatus.OPEN, OrderStatus.PENDING_PAYMENT);

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

    @Test
    @DisplayName("GET /api/orders/receivables — Deve listar recebíveis de forma paginada sem filtros opcionais")
    void shouldListReceivablesWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        ReceivableSummaryResponse summary = new ReceivableSummaryResponse(
                1L, 100L, null, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00), ReceivableStatus.PENDING, PaymentMethod.BOLETO, LocalDate.of(2026, 12, 1)
        );
        Page<ReceivableSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderService.listReceivables(null, null, null, null, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<ReceivableSummaryResponse>> response = orderController.listReceivables(null, null, null, null, pageable, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(orderService).listReceivables(null, null, null, null, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/orders/receivables — Deve listar recebíveis de forma paginada com filtros opcionais")
    void shouldListReceivablesWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<ReceivableStatus> statuses = List.of(ReceivableStatus.PENDING);

        ReceivableSummaryResponse summary = new ReceivableSummaryResponse(
                1L, 100L, null, "12345678901", "Cliente Teste", BigDecimal.valueOf(100.00), BigDecimal.valueOf(50.00), ReceivableStatus.PARTIALLY_PAID, PaymentMethod.PIX, LocalDate.of(2026, 11, 15)
        );
        Page<ReceivableSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(orderService.listReceivables("Cliente", startDate, endDate, statuses, 10L, pageable)).thenReturn(page);

        ResponseEntity<Page<ReceivableSummaryResponse>> response = orderController.listReceivables("Cliente", startDate, endDate, statuses, pageable, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(orderService).listReceivables("Cliente", startDate, endDate, statuses, 10L, pageable);
    }
}
