package company.aura.erp.service;

import company.aura.erp.dto.serviceorder.ServiceOrderItemCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderItemUpdateRequest;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.enums.ServiceOrderItemStatus;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.ServiceOrderItemRepository;
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
class ServiceOrderItemServiceTest {

    @Mock
    private ServiceOrderItemRepository serviceOrderItemRepository;

    @Mock
    private CatalogItemRepository catalogItemRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @InjectMocks
    private ServiceOrderItemService serviceOrderItemService;

    private ServiceOrderEntity serviceOrder;
    private CatalogItemEntity catalogItem;
    private CollaboratorEntity collaborator;

    @BeforeEach
    void setUp() {
        serviceOrder = ServiceOrderEntity.builder()
                .id(1L)
                .companyId(10L)
                .build();

        catalogItem = CatalogItemEntity.builder()
                .id(100L)
                .companyId(10L)
                .type(CatalogItemType.SERVICE)
                .unitPrice(BigDecimal.valueOf(120.00))
                .build();

        collaborator = CollaboratorEntity.builder()
                .id(20L)
                .companyId(10L)
                .status("ACTIVE")
                .name("Técnico Teste")
                .build();
    }

    @Test
    @DisplayName("Deve criar item com item de catálogo, profissional vinculado e status padrão PENDING")
    void shouldCreateItemWithCatalogAndProfessionalSuccessfully() {
        ServiceOrderItemCreateRequest request = ServiceOrderItemCreateRequest.builder()
                .catalogItemId(100L)
                .description("Formatação e backup")
                .quantity(BigDecimal.ONE)
                .discountAmount(BigDecimal.valueOf(20.00))
                .professionalId(20L)
                .build();

        when(catalogItemRepository.findByIdAndCompanyId(100L, 10L)).thenReturn(Optional.of(catalogItem));
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(20L, 10L, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));
        when(serviceOrderItemRepository.save(any(ServiceOrderItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderItemEntity item = serviceOrderItemService.createItem(request, serviceOrder);

        assertNotNull(item);
        assertEquals(CatalogItemType.SERVICE, item.getItemType());
        assertEquals(ServiceOrderItemStatus.PENDING, item.getStatus());
        assertEquals(BigDecimal.valueOf(120.00), item.getUnitPrice());
        assertEquals(BigDecimal.valueOf(100.00), item.getTotalPrice());
        assertEquals(20L, item.getProfessionalId());
        assertFalse(item.getStockReserved());
    }

    @Test
    @DisplayName("Deve criar item avulso sem catalogItemId com status customizado quando informado")
    void shouldCreateAdhocItemWithoutCatalogItemId() {
        ServiceOrderItemCreateRequest request = ServiceOrderItemCreateRequest.builder()
                .itemType(CatalogItemType.PHYSICAL)
                .status(ServiceOrderItemStatus.APPROVED)
                .description("Parafuso especial")
                .quantity(BigDecimal.valueOf(2))
                .unitPrice(BigDecimal.valueOf(15.00))
                .discountAmount(BigDecimal.ZERO)
                .stockReserved(true)
                .build();

        when(serviceOrderItemRepository.save(any(ServiceOrderItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderItemEntity item = serviceOrderItemService.createItem(request, serviceOrder);

        assertNotNull(item);
        assertNull(item.getCatalogItemId());
        assertEquals(CatalogItemType.PHYSICAL, item.getItemType());
        assertEquals(ServiceOrderItemStatus.APPROVED, item.getStatus());
        assertEquals(BigDecimal.valueOf(30.00), item.getTotalPrice());
        assertTrue(item.getStockReserved());
    }

    @Test
    @DisplayName("Deve falhar ao criar item diretamente com status CANCELED")
    void shouldFailWhenCreatingItemWithCanceledStatus() {
        ServiceOrderItemCreateRequest request = ServiceOrderItemCreateRequest.builder()
                .itemType(CatalogItemType.SERVICE)
                .status(ServiceOrderItemStatus.CANCELED)
                .description("Serviço inválido")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.TEN)
                .build();

        assertThrows(ResponseStatusException.class, () -> serviceOrderItemService.createItem(request, serviceOrder));
        verify(serviceOrderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao criar item sem catalogItemId e sem itemType ou unitPrice")
    void shouldFailWhenAdhocItemMissingTypeOrPrice() {
        ServiceOrderItemCreateRequest request = ServiceOrderItemCreateRequest.builder()
                .description("Item sem tipo")
                .quantity(BigDecimal.ONE)
                .build();

        assertThrows(ResponseStatusException.class, () -> serviceOrderItemService.createItem(request, serviceOrder));
        verify(serviceOrderItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar item da OS incluindo status e recalcular totalPrice")
    void shouldUpdateServiceOrderItemSuccessfully() {
        ServiceOrderItemEntity existing = ServiceOrderItemEntity.builder()
                .id(5L)
                .serviceOrderId(1L)
                .catalogItemId(100L)
                .itemType(CatalogItemType.SERVICE)
                .status(ServiceOrderItemStatus.PENDING)
                .description("Formatação")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(120.00))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(120.00))
                .stockReserved(false)
                .build();

        ServiceOrderItemUpdateRequest request = ServiceOrderItemUpdateRequest.builder()
                .status(ServiceOrderItemStatus.IN_PROGRESS)
                .quantity(BigDecimal.valueOf(2))
                .discountAmount(BigDecimal.valueOf(40.00))
                .professionalId(20L)
                .description("Formatação completa")
                .build();

        when(serviceOrderItemRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(20L, 10L, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(collaborator));
        when(serviceOrderItemRepository.save(any(ServiceOrderItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderItemEntity updated = serviceOrderItemService.updateServiceOrderItem(serviceOrder, 5L, request);

        assertEquals(ServiceOrderItemStatus.IN_PROGRESS, updated.getStatus());
        assertEquals(BigDecimal.valueOf(200.00), updated.getTotalPrice());
        assertEquals("Formatação completa", updated.getDescription());
        assertEquals(20L, updated.getProfessionalId());
    }

    @Test
    @DisplayName("Deve cancelar itens e liberar estoque reservado ao cancelar OS")
    void shouldCancelItemsAndReleaseReservedStock() {
        ServiceOrderItemEntity reservedItem = ServiceOrderItemEntity.builder()
                .id(10L)
                .serviceOrderId(1L)
                .status(ServiceOrderItemStatus.IN_PROGRESS)
                .stockReserved(true)
                .build();

        when(serviceOrderItemRepository.findByServiceOrderIdAndStatusNotIn(1L)).thenReturn(List.of(reservedItem));

        serviceOrderItemService.cancelItems(1L);

        assertFalse(reservedItem.getStockReserved());
        assertEquals(ServiceOrderItemStatus.CANCELED, reservedItem.getStatus());
        verify(serviceOrderItemRepository).save(reservedItem);
    }

    @Test
    @DisplayName("Deve marcar itens ativos como COMPLETED ao concluir OS")
    void shouldCompleteActiveItems() {
        ServiceOrderItemEntity activeItem = ServiceOrderItemEntity.builder()
                .id(10L)
                .serviceOrderId(1L)
                .status(ServiceOrderItemStatus.IN_PROGRESS)
                .build();
        ServiceOrderItemEntity canceledItem = ServiceOrderItemEntity.builder()
                .id(11L)
                .serviceOrderId(1L)
                .status(ServiceOrderItemStatus.CANCELED)
                .build();

        serviceOrderItemService.completeItems(List.of(activeItem, canceledItem));

        assertEquals(ServiceOrderItemStatus.COMPLETED, activeItem.getStatus());
        assertEquals(ServiceOrderItemStatus.CANCELED, canceledItem.getStatus());
        verify(serviceOrderItemRepository, times(1)).save(activeItem);
        verify(serviceOrderItemRepository, never()).save(canceledItem);
    }
}
