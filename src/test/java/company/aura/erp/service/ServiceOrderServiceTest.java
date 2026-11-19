package company.aura.erp.service;

import company.aura.erp.dto.order.OrderCreateRequest;
import company.aura.erp.dto.order.OrderResponse;
import company.aura.erp.dto.serviceorder.*;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.*;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.PartnerRepository;
import company.aura.erp.repository.ServiceOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
class ServiceOrderServiceTest {

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private ServiceOrderItemService serviceOrderItemService;

    @Mock
    private ServiceOrderTermService serviceOrderTermService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private ServiceOrderService serviceOrderService;

    private final Long companyId = 1L;
    private final Long partnerId = 10L;
    private final Long technicianId = 20L;
    private final Long termId = 30L;
    private ServiceOrderEntity sampleServiceOrder;

    @BeforeEach
    void setUp() {
        sampleServiceOrder = ServiceOrderEntity.builder()
                .id(100L)
                .companyId(companyId)
                .partnerId(partnerId)
                .technicianId(technicianId)
                .termId(termId)
                .status(ServiceOrderStatus.TRIAGE)
                .objectTitle("Notebook Dell Inspiron")
                .objectIdentifier("SN-DELL-01")
                .reportedIssue("Não carrega a bateria")
                .servicesAmount(BigDecimal.ZERO)
                .productsAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .entryAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve criar ordem de serviço com itens calculados e termo de condição")
    void shouldCreateServiceOrderWithItemsSuccessfully() {
        ServiceOrderCreateRequest request = ServiceOrderCreateRequest.builder()
                .partnerId(partnerId)
                .technicianId(technicianId)
                .termId(termId)
                .objectTitle("Notebook Dell Inspiron")
                .objectIdentifier("SN-DELL-01")
                .reportedIssue("Não carrega a bateria")
                .items(List.of(
                        ServiceOrderItemCreateRequest.builder()
                                .catalogItemId(1L)
                                .description("Diagnóstico")
                                .quantity(BigDecimal.ONE)
                                .discountAmount(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of(PartnerEntity.builder().id(partnerId).companyId(companyId).name("Cliente Teste").build()));
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(technicianId, companyId, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(CollaboratorEntity.builder().id(technicianId).companyId(companyId).name("Técnico João").build()));
        when(serviceOrderRepository.save(any(ServiceOrderEntity.class))).thenAnswer(inv -> {
            ServiceOrderEntity entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(100L);
            }
            return entity;
        });

        ServiceOrderItemEntity savedItem = ServiceOrderItemEntity.builder()
                .id(200L)
                .serviceOrderId(100L)
                .itemType(CatalogItemType.SERVICE)
                .status(ServiceOrderItemStatus.PENDING)
                .description("Diagnóstico")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(80.00))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(80.00))
                .build();

        when(serviceOrderItemService.saveInitialItems(eq(request), any(ServiceOrderEntity.class)))
                .thenReturn(List.of(savedItem));

        ServiceOrderResponse response = serviceOrderService.create(request, companyId);

        assertNotNull(response);
        assertEquals(ServiceOrderStatus.TRIAGE, response.getStatus());
        assertEquals(termId, response.getTermId());
        assertEquals("Cliente Teste", response.getCustomerName());
        assertEquals("Técnico João", response.getTechnicianName());
        assertEquals(BigDecimal.valueOf(80.00), response.getServicesAmount());
        assertEquals(BigDecimal.valueOf(80.00), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(ServiceOrderItemStatus.PENDING, response.getItems().get(0).getStatus());
        verify(serviceOrderTermService).validateTermExists(termId, companyId);
    }

    @Test
    @DisplayName("Deve buscar OS por ID retornando cliente, técnico e itens")
    void shouldFindServiceOrderById() {
        when(serviceOrderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleServiceOrder));
        when(partnerRepository.findNameByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of("Cliente Teste"));
        when(collaboratorRepository.findNameByIdAndCompanyId(technicianId, companyId))
                .thenReturn(Optional.of("Técnico João"));
        when(serviceOrderItemService.findServiceOrderItems(100L)).thenReturn(Collections.emptyList());

        ServiceOrderResponse response = serviceOrderService.findById(100L, companyId);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(termId, response.getTermId());
        assertEquals("Cliente Teste", response.getCustomerName());
        assertEquals("Técnico João", response.getTechnicianName());
    }

    @Test
    @DisplayName("Deve atualizar OS com termId e impedir alteração direta para COMPLETED/CANCELED")
    void shouldUpdateServiceOrderAndBlockTerminalStatusChange() {
        Long newTermId = 31L;
        ServiceOrderUpdateRequest validRequest = ServiceOrderUpdateRequest.builder()
                .partnerId(partnerId)
                .technicianId(technicianId)
                .termId(newTermId)
                .status(ServiceOrderStatus.IN_PROGRESS)
                .objectTitle("Notebook Dell Inspiron")
                .reportedIssue("Não carrega a bateria")
                .technicalReport("Conector DC danificado")
                .build();

        when(serviceOrderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleServiceOrder));
        when(serviceOrderRepository.save(any(ServiceOrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partnerRepository.findByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of(PartnerEntity.builder().id(partnerId).name("Cliente Teste").build()));
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(technicianId, companyId, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(CollaboratorEntity.builder().id(technicianId).name("Técnico João").build()));
        when(serviceOrderItemService.findServiceOrderItems(100L)).thenReturn(Collections.emptyList());

        ServiceOrderResponse updated = serviceOrderService.update(100L, validRequest, companyId);
        assertEquals(ServiceOrderStatus.IN_PROGRESS, updated.getStatus());
        assertEquals("Conector DC danificado", updated.getTechnicalReport());
        assertEquals(newTermId, updated.getTermId());
        verify(serviceOrderTermService).validateTermExists(newTermId, companyId);

        ServiceOrderUpdateRequest invalidRequest = ServiceOrderUpdateRequest.builder()
                .status(ServiceOrderStatus.COMPLETED)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> serviceOrderService.update(100L, invalidRequest, companyId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve adicionar, atualizar (inclusive cancelar) e remover item da OS recalculando totais")
    void shouldManageItemsAndRecalculateTotals() {
        ServiceOrderItemCreateRequest createReq = ServiceOrderItemCreateRequest.builder()
                .catalogItemId(1L)
                .description("Troca de bateria")
                .quantity(BigDecimal.ONE)
                .discountAmount(BigDecimal.valueOf(10.00))
                .build();

        ServiceOrderItemEntity createdItem = ServiceOrderItemEntity.builder()
                .id(201L)
                .serviceOrderId(100L)
                .itemType(CatalogItemType.PHYSICAL)
                .status(ServiceOrderItemStatus.PENDING)
                .description("Troca de bateria")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(210.00))
                .discountAmount(BigDecimal.valueOf(10.00))
                .totalPrice(BigDecimal.valueOf(200.00))
                .build();

        when(serviceOrderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleServiceOrder));
        when(serviceOrderItemService.createItem(createReq, sampleServiceOrder)).thenReturn(createdItem);
        when(serviceOrderItemService.findServiceOrderItems(100L)).thenReturn(List.of(createdItem));
        when(serviceOrderRepository.save(sampleServiceOrder)).thenReturn(sampleServiceOrder);

        ServiceOrderItemResponse added = serviceOrderService.addItem(100L, createReq, companyId);
        assertEquals(ServiceOrderItemStatus.PENDING, added.getStatus());
        assertEquals(BigDecimal.valueOf(200.00), added.getTotalPrice());
        assertEquals(BigDecimal.valueOf(200.00), sampleServiceOrder.getProductsAmount());
        assertEquals(BigDecimal.valueOf(10.00), sampleServiceOrder.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(200.00), sampleServiceOrder.getTotalAmount());

        ServiceOrderItemUpdateRequest updateReq = ServiceOrderItemUpdateRequest.builder()
                .status(ServiceOrderItemStatus.CANCELED)
                .build();
        createdItem.setStatus(ServiceOrderItemStatus.CANCELED);
        when(serviceOrderItemService.updateServiceOrderItem(sampleServiceOrder, 201L, updateReq)).thenReturn(createdItem);

        ServiceOrderItemResponse updated = serviceOrderService.updateItem(100L, 201L, updateReq, companyId);
        assertEquals(ServiceOrderItemStatus.CANCELED, updated.getStatus());
        assertEquals(BigDecimal.ZERO, sampleServiceOrder.getTotalAmount());

        when(serviceOrderItemService.findServiceOrderItems(100L)).thenReturn(Collections.emptyList());
        serviceOrderService.removeItem(100L, 201L, companyId);
        assertEquals(BigDecimal.ZERO, sampleServiceOrder.getTotalAmount());
        verify(serviceOrderItemService).cancelItem(100L, 201L);
    }

    @Test
    @DisplayName("Deve cancelar OS e cancelar seus itens liberando estoque reservado")
    void shouldCancelServiceOrder() {
        when(serviceOrderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleServiceOrder));
        when(serviceOrderRepository.save(any(ServiceOrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partnerRepository.findNameByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of("Cliente Teste"));
        when(collaboratorRepository.findNameByIdAndCompanyId(technicianId, companyId))
                .thenReturn(Optional.of("Técnico João"));
        when(serviceOrderItemService.findServiceOrderItems(100L)).thenReturn(Collections.emptyList());

        ServiceOrderResponse response = serviceOrderService.cancel(100L, companyId);

        assertEquals(ServiceOrderStatus.CANCELED, response.getStatus());
        verify(serviceOrderItemService).cancelItems(100L);
    }

    @Test
    @DisplayName("Deve finalizar OS mudando status para COMPLETED e abrindo pedido com os itens ativos")
    void shouldCompleteServiceOrderAndOpenOrder() {
        ServiceOrderItemEntity serviceItem = ServiceOrderItemEntity.builder()
                .id(201L)
                .serviceOrderId(100L)
                .catalogItemId(10L)
                .itemType(CatalogItemType.SERVICE)
                .status(ServiceOrderItemStatus.IN_PROGRESS)
                .description("Mão de obra")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(150.00))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(150.00))
                .build();

        ServiceOrderItemEntity physicalItem = ServiceOrderItemEntity.builder()
                .id(202L)
                .serviceOrderId(100L)
                .catalogItemId(11L)
                .itemType(CatalogItemType.PHYSICAL)
                .status(ServiceOrderItemStatus.APPROVED)
                .description("Bateria nova")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(250.00))
                .discountAmount(BigDecimal.valueOf(20.00))
                .totalPrice(BigDecimal.valueOf(230.00))
                .build();

        List<ServiceOrderItemEntity> activeItems = List.of(serviceItem, physicalItem);

        sampleServiceOrder.setTechnicalReport("Bateria substituída e testada");

        ServiceOrderCompleteRequest completeRequest = ServiceOrderCompleteRequest.builder()
                .orderNotes("Pedido referente à OS #1")
                .build();

        OrderResponse openedOrder = OrderResponse.builder()
                .id(500L)
                .companyId(companyId)
                .partnerId(partnerId)
                .status(OrderStatus.OPEN)
                .servicesAmount(BigDecimal.valueOf(150.00))
                .productsAmount(BigDecimal.valueOf(230.00))
                .discountAmount(BigDecimal.valueOf(20.00))
                .totalAmount(BigDecimal.valueOf(380.00))
                .build();

        when(serviceOrderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleServiceOrder));
        when(serviceOrderItemService.findServiceOrderItems(100L)).thenReturn(activeItems);
        when(orderService.openOrder(any(OrderCreateRequest.class), eq(companyId)))
                .thenReturn(openedOrder);
        when(serviceOrderRepository.save(any(ServiceOrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partnerRepository.findNameByIdAndCompanyId(partnerId, companyId))
                .thenReturn(Optional.of("Cliente Teste"));
        when(collaboratorRepository.findNameByIdAndCompanyId(technicianId, companyId))
                .thenReturn(Optional.of("Técnico João"));

        ServiceOrderResponse response = serviceOrderService.complete(100L, completeRequest, companyId);

        assertNotNull(response);
        assertEquals(ServiceOrderStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getCompletedAt());
        assertEquals(500L, response.getOrderId());
        assertEquals(BigDecimal.valueOf(380.00), response.getTotalAmount());
        assertEquals("Bateria substituída e testada", response.getTechnicalReport());
        verify(serviceOrderItemService).completeItems(activeItems);

        ArgumentCaptor<OrderCreateRequest> orderRequestCaptor = ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(orderService).openOrder(orderRequestCaptor.capture(), eq(companyId));
        assertEquals(partnerId, orderRequestCaptor.getValue().partnerId());
        assertEquals("Pedido referente à OS #1", orderRequestCaptor.getValue().notes());
        assertEquals(2, orderRequestCaptor.getValue().items().size());
    }

    @Test
    @DisplayName("Deve impedir finalização de OS já concluída ou cancelada")
    void shouldThrowBadRequestWhenCompletingAlreadyCompletedOrCanceled() {
        sampleServiceOrder.setStatus(ServiceOrderStatus.CANCELED);
        when(serviceOrderRepository.findByIdAndCompanyId(100L, companyId)).thenReturn(Optional.of(sampleServiceOrder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> serviceOrderService.complete(100L, null, companyId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Deve listar ordens de serviço resumidas com filtros")
    void shouldListSummarizedWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);
        List<ServiceOrderStatus> statuses = List.of(ServiceOrderStatus.TRIAGE);

        ServiceOrderSummaryResponse summary = ServiceOrderSummaryResponse.builder()
                .id(100L)
                .objectTitle("Notebook Dell Inspiron")
                .status(ServiceOrderStatus.TRIAGE)
                .build();
        Page<ServiceOrderSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(serviceOrderRepository.searchSummarized(
                eq(companyId), eq("Dell"), eq(true), eq(false), eq(false),
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(true), eq(statuses), eq(partnerId), eq(technicianId), eq("SN-DELL-01"), eq(pageable)
        )).thenReturn(page);

        Page<ServiceOrderSummaryResponse> result = serviceOrderService.listSummarized(
                "Dell", startDate, endDate, statuses, partnerId, technicianId, "SN-DELL-01", companyId, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());
    }
}
