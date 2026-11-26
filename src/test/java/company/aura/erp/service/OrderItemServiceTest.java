package company.aura.erp.service;

import company.aura.erp.dto.order.OrderCreateRequest;
import company.aura.erp.dto.order.OrderItemCreateRequest;
import company.aura.erp.dto.order.OrderItemUpdateRequest;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    private OrderEntity order;
    private CatalogItemEntity catalogItem;
    private CollaboratorEntity collaborator;

    @BeforeEach
    void setUp() {
        order = OrderEntity.builder()
                .id(1L)
                .companyId(10L)
                .build();

        catalogItem = CatalogItemEntity.builder()
                .id(100L)
                .companyId(10L)
                .type(CatalogItemType.SERVICE)
                .unitPrice(BigDecimal.valueOf(50.00))
                .build();

        collaborator = CollaboratorEntity.builder()
                .id(20L)
                .companyId(10L)
                .status("ACTIVE")
                .name("Colaborador Teste")
                .build();
    }

    @Test
    @DisplayName("Deve criar item com profissional vinculado com sucesso quando colaborador existe")
    void shouldCreateItemWithProfessionalSuccessfully() {
        OrderItemCreateRequest request = new OrderItemCreateRequest(
                100L,
                "Corte de Cabelo",
                null,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                20L
        );

        when(catalogItemRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(catalogItem));
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(20L, 10L, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItemEntity item = orderItemService.createItem(request, order);

        assertNotNull(item);
        assertEquals(20L, item.getProfessionalId());
        assertEquals(BigDecimal.valueOf(50.00), item.getUnitPrice());
        assertEquals(BigDecimal.valueOf(50.00), item.getTotalPrice());
        verify(collaboratorRepository).findByIdAndCompanyIdAndStatusNot(20L, 10L, CollaboratorStatus.DELETED.name());
    }

    @Test
    @DisplayName("Deve criar item usando unitPrice customizado quando informado na request")
    void shouldCreateItemWithCustomUnitPrice() {
        OrderItemCreateRequest request = OrderItemCreateRequest.builder()
                .catalogItemId(100L)
                .description("Corte de Cabelo Especial")
                .unitPrice(new BigDecimal("85.00"))
                .quantity(new BigDecimal("2"))
                .discountAmount(new BigDecimal("10.00"))
                .build();

        when(catalogItemRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(catalogItem));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItemEntity item = orderItemService.createItem(request, order);

        assertNotNull(item);
        assertEquals(new BigDecimal("85.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("160.00"), item.getTotalPrice());
    }

    @Test
    @DisplayName("Deve salvar itens iniciais na criação de pedido respeitando unitPrice customizado quando informado")
    void shouldSaveInitialItemsWithCustomUnitPrice() {
        OrderCreateRequest orderRequest = OrderCreateRequest.builder()
                .partnerId(1L)
                .items(List.of(
                        OrderItemCreateRequest.builder()
                                .catalogItemId(100L)
                                .description("Corte VIP")
                                .unitPrice(new BigDecimal("75.00"))
                                .quantity(new BigDecimal("2"))
                                .discountAmount(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        when(catalogItemRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(catalogItem));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<OrderItemEntity> items = orderItemService.saveInitialItems(orderRequest, order);

        assertEquals(1, items.size());
        assertEquals(new BigDecimal("75.00"), items.get(0).getUnitPrice());
        assertEquals(new BigDecimal("150.00"), items.get(0).getTotalPrice());
    }

    @Test
    @DisplayName("Deve lançar erro ao criar item se o colaborador profissional não existir ou estiver deletado")
    void shouldFailWhenProfessionalCollaboratorNotFoundOnCreate() {
        OrderItemCreateRequest request = new OrderItemCreateRequest(
                100L,
                "Corte de Cabelo",
                null,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                999L
        );

        when(catalogItemRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(catalogItem));
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(999L, 10L, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> orderItemService.createItem(request, order));
        verify(orderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar item com profissional vinculado com sucesso")
    void shouldUpdateItemWithProfessionalSuccessfully() {
        OrderItemEntity existingItem = OrderItemEntity.builder()
                .id(5L)
                .orderId(1L)
                .catalogItemId(100L)
                .itemType(CatalogItemType.SERVICE)
                .unitPrice(BigDecimal.valueOf(50.00))
                .quantity(BigDecimal.ONE)
                .totalPrice(BigDecimal.valueOf(50.00))
                .build();

        OrderItemUpdateRequest request = new OrderItemUpdateRequest(
                null,
                BigDecimal.valueOf(2),
                BigDecimal.ZERO,
                20L,
                "Corte de Cabelo Duplo"
        );

        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(existingItem));
        when(catalogItemRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(catalogItem));
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(20L, 10L, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItemEntity updated = orderItemService.updateOrderItem(order, 5L, request);

        assertNotNull(updated);
        assertEquals(20L, updated.getProfessionalId());
        assertEquals(BigDecimal.valueOf(50.00), updated.getUnitPrice());
        assertEquals(BigDecimal.valueOf(100.00), updated.getTotalPrice());
    }

    @Test
    @DisplayName("Deve atualizar unitPrice do item e recalcular totalPrice na edição")
    void shouldUpdateItemUnitPriceAndRecalculateTotalPrice() {
        OrderItemEntity existingItem = OrderItemEntity.builder()
                .id(5L)
                .orderId(1L)
                .catalogItemId(100L)
                .itemType(CatalogItemType.SERVICE)
                .description("Corte de Cabelo")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(new BigDecimal("2"))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("100.00"))
                .build();

        OrderItemUpdateRequest request = OrderItemUpdateRequest.builder()
                .unitPrice(new BigDecimal("65.00"))
                .quantity(new BigDecimal("3"))
                .discountAmount(new BigDecimal("15.00"))
                .description("Corte de Cabelo Ajustado")
                .build();

        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(existingItem));
        when(catalogItemRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(catalogItem));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItemEntity updated = orderItemService.updateOrderItem(order, 5L, request);

        assertNotNull(updated);
        assertEquals(new BigDecimal("65.00"), updated.getUnitPrice());
        assertEquals(new BigDecimal("180.00"), updated.getTotalPrice());
    }

    @Test
    @DisplayName("Deve preservar unitPrice customizado do item quando unitPrice e novo catalogItemId não forem informados na edição")
    void shouldPreserveCustomUnitPriceWhenUnitPriceNotProvidedAndCatalogUnchanged() {
        OrderItemEntity existingItem = OrderItemEntity.builder()
                .id(5L)
                .orderId(1L)
                .catalogItemId(100L)
                .itemType(CatalogItemType.SERVICE)
                .description("Corte VIP")
                .unitPrice(new BigDecimal("80.00"))
                .quantity(BigDecimal.ONE)
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("80.00"))
                .build();

        OrderItemUpdateRequest request = OrderItemUpdateRequest.builder()
                .quantity(new BigDecimal("2"))
                .discountAmount(BigDecimal.ZERO)
                .build();

        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(existingItem));
        when(catalogItemRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(catalogItem));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItemEntity updated = orderItemService.updateOrderItem(order, 5L, request);

        assertEquals(new BigDecimal("80.00"), updated.getUnitPrice());
        assertEquals(new BigDecimal("160.00"), updated.getTotalPrice());
        assertEquals("Corte VIP", updated.getDescription());
    }

    @Test
    @DisplayName("Deve usar unitPrice do novo item de catálogo quando catalogItemId mudar e unitPrice não for informado")
    void shouldUseNewCatalogItemUnitPriceWhenCatalogItemChangesWithoutUnitPrice() {
        CatalogItemEntity anotherCatalogItem = CatalogItemEntity.builder()
                .id(200L)
                .companyId(10L)
                .type(CatalogItemType.PHYSICAL)
                .unitPrice(new BigDecimal("120.00"))
                .build();

        OrderItemEntity existingItem = OrderItemEntity.builder()
                .id(5L)
                .orderId(1L)
                .catalogItemId(100L)
                .itemType(CatalogItemType.SERVICE)
                .description("Corte VIP")
                .unitPrice(new BigDecimal("80.00"))
                .quantity(BigDecimal.ONE)
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("80.00"))
                .build();

        OrderItemUpdateRequest request = OrderItemUpdateRequest.builder()
                .catalogItemId(200L)
                .quantity(new BigDecimal("2"))
                .discountAmount(BigDecimal.ZERO)
                .description("Produto Trocado")
                .build();

        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(existingItem));
        when(catalogItemRepository.findByIdAndCompanyId(200L, 10L)).thenReturn(Optional.of(anotherCatalogItem));
        when(orderItemRepository.save(any(OrderItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderItemEntity updated = orderItemService.updateOrderItem(order, 5L, request);

        assertEquals(200L, updated.getCatalogItemId());
        assertEquals(CatalogItemType.PHYSICAL, updated.getItemType());
        assertEquals(new BigDecimal("120.00"), updated.getUnitPrice());
        assertEquals(new BigDecimal("240.00"), updated.getTotalPrice());
    }
}
