package company.aura.erp.controller;

import company.aura.erp.dto.serviceorder.*;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.ServiceOrderItemStatus;
import company.aura.erp.enums.ServiceOrderStatus;
import company.aura.erp.enums.ServiceOrderTermStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.ServiceOrderService;
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
class ServiceOrderControllerTest {

    @Mock
    private ServiceOrderService serviceOrderService;

    @InjectMocks
    private ServiceOrderController serviceOrderController;

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
    @DisplayName("GET /api/service-orders — Deve listar ordens de serviço resumidas e filtradas")
    void shouldListSummarizedServiceOrders() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<ServiceOrderStatus> statuses = List.of(ServiceOrderStatus.TRIAGE, ServiceOrderStatus.IN_PROGRESS);

        ServiceOrderSummaryResponse summary = ServiceOrderSummaryResponse.builder()
                .id(1L)
                .partnerId(5L)
                .customerDocument("12345678901")
                .customerName("Cliente Teste")
                .technicianId(3L)
                .technicianName("Técnico Teste")
                .objectTitle("Notebook Dell")
                .objectIdentifier("SN12345")
                .totalAmount(BigDecimal.valueOf(250.00))
                .status(ServiceOrderStatus.TRIAGE)
                .entryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        Page<ServiceOrderSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(serviceOrderService.listSummarized("Dell", startDate, endDate, statuses, 5L, 3L, "SN12345", 10L, pageable))
                .thenReturn(page);

        ResponseEntity<Page<ServiceOrderSummaryResponse>> response = serviceOrderController.list(
                "Dell", startDate, endDate, statuses, 5L, 3L, "SN12345", pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(serviceOrderService).listSummarized("Dell", startDate, endDate, statuses, 5L, 3L, "SN12345", 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/service-orders/{id} — Deve retornar OS por ID")
    void shouldFindServiceOrderById() {
        ServiceOrderResponse responseDto = ServiceOrderResponse.builder()
                .id(1L)
                .companyId(10L)
                .orderNumber(1)
                .partnerId(5L)
                .customerName("Cliente Teste")
                .termId(7L)
                .status(ServiceOrderStatus.TRIAGE)
                .objectTitle("Notebook Dell")
                .reportedIssue("Não liga")
                .items(Collections.emptyList())
                .build();

        when(serviceOrderService.findById(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<ServiceOrderResponse> response = serviceOrderController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(serviceOrderService).findById(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/service-orders — Deve criar OS com termId e retornar HTTP 201")
    void shouldCreateServiceOrder() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .partnerId(5L)
                .technicianId(3L)
                .termId(7L)
                .objectTitle("Notebook Dell")
                .objectIdentifier("SN12345")
                .reportedIssue("Não liga")
                .items(Collections.emptyList())
                .build();

        ServiceOrderResponse responseDto = ServiceOrderResponse.builder()
                .id(1L)
                .companyId(10L)
                .orderNumber(1)
                .partnerId(5L)
                .technicianId(3L)
                .termId(7L)
                .status(ServiceOrderStatus.TRIAGE)
                .objectTitle("Notebook Dell")
                .reportedIssue("Não liga")
                .build();

        when(serviceOrderService.create(request, 10L)).thenReturn(responseDto);

        ResponseEntity<ServiceOrderResponse> response = serviceOrderController.create(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        assertEquals(7L, response.getBody().getTermId());
        verify(serviceOrderService).create(request, 10L);
    }

    @Test
    @DisplayName("PUT /api/service-orders/{id} — Deve atualizar OS com termId e retornar HTTP 200")
    void shouldUpdateServiceOrder() {
        ServiceOrderUpdateRequest request = ServiceOrderUpdateRequest.builder()
                .partnerId(5L)
                .termId(8L)
                .objectTitle("Notebook Dell")
                .reportedIssue("Não liga")
                .status(ServiceOrderStatus.IN_PROGRESS)
                .technicalReport("Placa mãe com defeito")
                .build();

        ServiceOrderResponse responseDto = ServiceOrderResponse.builder()
                .id(1L)
                .companyId(10L)
                .termId(8L)
                .status(ServiceOrderStatus.IN_PROGRESS)
                .technicalReport("Placa mãe com defeito")
                .build();

        when(serviceOrderService.update(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<ServiceOrderResponse> response = serviceOrderController.update(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        assertEquals(8L, response.getBody().getTermId());
        verify(serviceOrderService).update(1L, request, 10L);
    }

    @Test
    @DisplayName("GET /api/service-orders/terms — Deve listar termos paginados")
    void shouldListServiceOrderTerms() {
        Pageable pageable = PageRequest.of(0, 20);
        ServiceOrderTermSummaryResponse summary = ServiceOrderTermSummaryResponse.builder()
                .id(1L)
                .title("Termo de Garantia")
                .status(ServiceOrderTermStatus.ACTIVE)
                .build();
        Page<ServiceOrderTermSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(serviceOrderService.listTerms("Garantia", ServiceOrderTermStatus.ACTIVE, 10L, pageable))
                .thenReturn(page);

        ResponseEntity<Page<ServiceOrderTermSummaryResponse>> response = serviceOrderController.listTerms(
                "Garantia", ServiceOrderTermStatus.ACTIVE, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(serviceOrderService).listTerms("Garantia", ServiceOrderTermStatus.ACTIVE, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/service-orders/terms/{id} — Deve retornar termo por ID")
    void shouldFindServiceOrderTermById() {
        ServiceOrderTermResponse termResponse = ServiceOrderTermResponse.builder()
                .id(1L)
                .companyId(10L)
                .title("Termo de Garantia")
                .content("Garantia de 90 dias.")
                .isDefault(true)
                .status(ServiceOrderTermStatus.ACTIVE)
                .build();

        when(serviceOrderService.findTermById(1L, 10L)).thenReturn(termResponse);

        ResponseEntity<ServiceOrderTermResponse> response = serviceOrderController.findTermById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(termResponse, response.getBody());
        verify(serviceOrderService).findTermById(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/service-orders/terms — Deve criar termo e retornar HTTP 201")
    void shouldCreateServiceOrderTerm() {
        ServiceOrderTermCreateRequest request = ServiceOrderTermCreateRequest.builder()
                .title("Termo de Garantia")
                .content("Garantia de 90 dias.")
                .isDefault(true)
                .build();

        ServiceOrderTermResponse termResponse = ServiceOrderTermResponse.builder()
                .id(1L)
                .companyId(10L)
                .title("Termo de Garantia")
                .content("Garantia de 90 dias.")
                .isDefault(true)
                .status(ServiceOrderTermStatus.ACTIVE)
                .build();

        when(serviceOrderService.createTerm(request, 10L)).thenReturn(termResponse);

        ResponseEntity<ServiceOrderTermResponse> response = serviceOrderController.createTerm(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(termResponse, response.getBody());
        verify(serviceOrderService).createTerm(request, 10L);
    }

    @Test
    @DisplayName("PUT /api/service-orders/terms/{id} — Deve atualizar termo e retornar HTTP 200")
    void shouldUpdateServiceOrderTerm() {
        ServiceOrderTermUpdateRequest request = ServiceOrderTermUpdateRequest.builder()
                .title("Termo Atualizado")
                .content("Conteúdo atualizado.")
                .isDefault(false)
                .status(ServiceOrderTermStatus.INACTIVE)
                .build();

        ServiceOrderTermResponse termResponse = ServiceOrderTermResponse.builder()
                .id(1L)
                .companyId(10L)
                .title("Termo Atualizado")
                .content("Conteúdo atualizado.")
                .isDefault(false)
                .status(ServiceOrderTermStatus.INACTIVE)
                .build();

        when(serviceOrderService.updateTerm(1L, request, 10L)).thenReturn(termResponse);

        ResponseEntity<ServiceOrderTermResponse> response = serviceOrderController.updateTerm(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(termResponse, response.getBody());
        verify(serviceOrderService).updateTerm(1L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/service-orders/terms/{id} — Deve apagar termo e retornar HTTP 204")
    void shouldDeleteServiceOrderTerm() {
        ResponseEntity<Void> response = serviceOrderController.deleteTerm(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(serviceOrderService).deleteTerm(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/service-orders/{id}/items — Deve adicionar item na OS e retornar HTTP 201")
    void shouldAddItemToServiceOrder() {
        ServiceOrderItemCreateRequest request = ServiceOrderItemCreateRequest.builder()
                .catalogItemId(2L)
                .status(ServiceOrderItemStatus.PENDING)
                .description("Troca de tela")
                .quantity(BigDecimal.ONE)
                .discountAmount(BigDecimal.ZERO)
                .build();

        ServiceOrderItemResponse itemResponse = ServiceOrderItemResponse.builder()
                .id(100L)
                .serviceOrderId(1L)
                .catalogItemId(2L)
                .itemType(CatalogItemType.SERVICE)
                .status(ServiceOrderItemStatus.PENDING)
                .description("Troca de tela")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(300.00))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(300.00))
                .stockReserved(false)
                .build();

        when(serviceOrderService.addItem(1L, request, 10L)).thenReturn(itemResponse);

        ResponseEntity<ServiceOrderItemResponse> response = serviceOrderController.addItem(1L, request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(itemResponse, response.getBody());
        verify(serviceOrderService).addItem(1L, request, 10L);
    }

    @Test
    @DisplayName("PUT /api/service-orders/{id}/items/{itemId} — Deve atualizar item da OS e retornar HTTP 200")
    void shouldUpdateItemInServiceOrder() {
        ServiceOrderItemUpdateRequest request = ServiceOrderItemUpdateRequest.builder()
                .catalogItemId(2L)
                .status(ServiceOrderItemStatus.IN_PROGRESS)
                .quantity(BigDecimal.valueOf(2))
                .discountAmount(BigDecimal.TEN)
                .description("Troca de tela atualizada")
                .build();

        ServiceOrderItemResponse itemResponse = ServiceOrderItemResponse.builder()
                .id(100L)
                .serviceOrderId(1L)
                .status(ServiceOrderItemStatus.IN_PROGRESS)
                .description("Troca de tela atualizada")
                .quantity(BigDecimal.valueOf(2))
                .totalPrice(BigDecimal.valueOf(590.00))
                .build();

        when(serviceOrderService.updateItem(1L, 100L, request, 10L)).thenReturn(itemResponse);

        ResponseEntity<ServiceOrderItemResponse> response = serviceOrderController.updateItem(1L, 100L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(itemResponse, response.getBody());
        verify(serviceOrderService).updateItem(1L, 100L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/service-orders/{id}/items/{itemId} — Deve apagar item da OS e retornar HTTP 204")
    void shouldRemoveItemFromServiceOrder() {
        ResponseEntity<Void> response = serviceOrderController.removeItem(1L, 100L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(serviceOrderService).removeItem(1L, 100L, 10L);
    }

    @Test
    @DisplayName("POST /api/service-orders/{id}/cancel — Deve cancelar OS e retornar HTTP 200")
    void shouldCancelServiceOrder() {
        ServiceOrderResponse responseDto = ServiceOrderResponse.builder()
                .id(1L)
                .companyId(10L)
                .status(ServiceOrderStatus.CANCELED)
                .build();

        when(serviceOrderService.cancel(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<ServiceOrderResponse> response = serviceOrderController.cancel(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        verify(serviceOrderService).cancel(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/service-orders/{id}/complete — Deve finalizar OS e abrir pedido")
    void shouldCompleteServiceOrder() {
        ServiceOrderCompleteRequest request = ServiceOrderCompleteRequest.builder()
                .orderNotes("Pedido referente à OS #1")
                .build();

        ServiceOrderResponse responseDto = ServiceOrderResponse.builder()
                .id(1L)
                .companyId(10L)
                .orderId(55L)
                .status(ServiceOrderStatus.COMPLETED)
                .technicalReport("Serviço concluído")
                .build();

        when(serviceOrderService.complete(1L, request, 10L)).thenReturn(responseDto);

        ResponseEntity<ServiceOrderResponse> response = serviceOrderController.complete(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseDto, response.getBody());
        assertEquals(55L, response.getBody().getOrderId());
        verify(serviceOrderService).complete(1L, request, 10L);
    }
}
