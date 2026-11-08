package company.aura.erp.service;

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
        assertEquals(BigDecimal.valueOf(50.00), item.getTotalPrice());
        verify(collaboratorRepository).findByIdAndCompanyIdAndStatusNot(20L, 10L, CollaboratorStatus.DELETED.name());
    }

    @Test
    @DisplayName("Deve lançar erro ao criar item se o colaborador profissional não existir ou estiver deletado")
    void shouldFailWhenProfessionalCollaboratorNotFoundOnCreate() {
        OrderItemCreateRequest request = new OrderItemCreateRequest(
                100L,
                "Corte de Cabelo",
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
        assertEquals(BigDecimal.valueOf(100.00), updated.getTotalPrice());
    }
}
