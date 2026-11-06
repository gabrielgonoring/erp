package company.aura.erp.service;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.dto.order.LinkOrderInvoice;
import company.aura.erp.dto.order.OrderExternalInvoiceRequest;
import company.aura.erp.dto.order.OrderInvoiceResponse;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInvoiceEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.InvoicePurpose;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.repository.OrderInvoiceRepository;
import company.aura.erp.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderInvoiceServiceTest {

    @Mock
    private OrderInvoiceRepository orderInvoiceRepository;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private OrderInvoiceService orderInvoiceService;

    private Long companyId = 10L;
    private Long orderId = 100L;
    private UserPrincipal userPrincipal;
    private OrderEntity orderEntity;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(companyId)
                .email("admin@empresa.com")
                .name("Admin")
                .authorities(Collections.emptyList())
                .build();

        orderEntity = OrderEntity.builder()
                .id(orderId)
                .companyId(companyId)
                .partnerId(5L)
                .status(OrderStatus.OPEN)
                .totalAmount(BigDecimal.valueOf(250.00))
                .build();
    }

    @Test
    @DisplayName("linkInvoice - Deve vincular nota fiscal existente ao pedido com sucesso")
    void shouldLinkInvoiceSuccess() {
        LinkOrderInvoice request = new LinkOrderInvoice(InvoicePurpose.PRODUCT, "Nota de produtos");

        when(orderInvoiceRepository.existsByOrderIdAndInvoiceIdAndInvoicePurpose(orderId, 200L, InvoicePurpose.PRODUCT))
                .thenReturn(false);

        when(orderInvoiceRepository.save(any(OrderInvoiceEntity.class))).thenAnswer(inv -> {
            OrderInvoiceEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        OrderInvoiceResponse response = orderInvoiceService.linkInvoice(200L, request, userPrincipal, orderEntity);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(companyId, response.getCompanyId());
        assertEquals(orderId, response.getOrderId());
        assertEquals(200L, response.getInvoiceId());
        assertEquals(InvoicePurpose.PRODUCT, response.getInvoicePurpose());
        assertEquals("Nota de produtos", response.getNotes());
        verify(orderInvoiceRepository).save(any(OrderInvoiceEntity.class));
    }

    @Test
    @DisplayName("linkInvoice - Deve lançar CONFLICT se nota já estiver vinculada com a mesma finalidade")
    void shouldThrowConflictWhenLinkInvoiceAlreadyExists() {
        LinkOrderInvoice request = new LinkOrderInvoice(InvoicePurpose.PRODUCT, "Nota duplicada");

        when(orderInvoiceRepository.existsByOrderIdAndInvoiceIdAndInvoicePurpose(orderId, 200L, InvoicePurpose.PRODUCT))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderInvoiceService.linkInvoice(200L, request, userPrincipal, orderEntity)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(orderInvoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("linkExternalInvoice - Deve vincular nota fiscal externa NFE com sucesso")
    void shouldLinkExternalInvoiceNfeSuccess() {
        MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "application/xml", "<xml/>".getBytes());
        MockMultipartFile pdfFile = new MockMultipartFile("pdfFile", "danfe.pdf", "application/pdf", "%PDF".getBytes());
        OrderExternalInvoiceRequest request = OrderExternalInvoiceRequest.builder()
                .documentType(DocumentType.NFE)
                .operationType(1)
                .totalAmount(BigDecimal.valueOf(250.00))
                .notes("Obs NFE")
                .build();

        InvoiceResponse invoiceResponse = InvoiceResponse.builder()
                .id(555L)
                .companyId(companyId)
                .partnerId(5L)
                .documentType(DocumentType.NFE)
                .totalAmount(BigDecimal.valueOf(250.00))
                .build();

        when(invoiceService.createExternalInvoice(any(InvoiceCreateRequest.class), eq(xmlFile), eq(pdfFile), eq(userPrincipal)))
                .thenReturn(invoiceResponse);

        InvoiceResponse response = orderInvoiceService.linkExternalInvoice(orderEntity, request, xmlFile, pdfFile, userPrincipal);

        assertNotNull(response);
        assertEquals(555L, response.getId());
        verify(orderInvoiceRepository).save(any(OrderInvoiceEntity.class));
    }

    @Test
    @DisplayName("linkExternalInvoice - Deve lançar BAD_REQUEST quando request ou documentType forem nulos")
    void shouldThrowBadRequestWhenDocumentTypeNull() {
        OrderExternalInvoiceRequest request = OrderExternalInvoiceRequest.builder()
                .documentType(null)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderInvoiceService.linkExternalInvoice(orderEntity, request, null, null, userPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("findInvoicesByOrderId - Deve retornar lista de notas do pedido")
    void shouldFindInvoicesByOrderId() {
        OrderInvoiceEntity entity = OrderInvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .orderId(orderId)
                .invoiceId(200L)
                .invoicePurpose(InvoicePurpose.PRODUCT)
                .build();

        when(orderInvoiceRepository.findByCompanyIdAndOrderId(companyId, orderId))
                .thenReturn(List.of(entity));

        List<OrderInvoiceResponse> result = orderInvoiceService.findInvoicesByOrderId(orderId, companyId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getInvoiceId());
        assertEquals(InvoicePurpose.PRODUCT, result.get(0).getInvoicePurpose());
    }
}
