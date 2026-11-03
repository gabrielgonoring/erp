package company.aura.erp.service;

import company.aura.erp.dto.order.OrderCloseRequest;
import company.aura.erp.dto.order.OrderInstallmentRequest;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.repository.OrderInstallmentRepository;
import company.aura.erp.repository.PaymentTransactionRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {

    @Mock
    private OrderInstallmentRepository orderInstallmentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @InjectMocks
    private OrderPaymentService orderPaymentService;

    private OrderEntity sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = OrderEntity.builder()
                .id(100L)
                .companyId(10L)
                .partnerId(5L)
                .status(OrderStatus.OPEN)
                .totalAmount(BigDecimal.valueOf(135.00))
                .build();
    }

    @Test
    @DisplayName("Deve processar parcelas com sucesso quando a soma bater exatamente com o total do pedido")
    void shouldProcessInstallmentsSuccessfully() {
        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new OrderInstallmentRequest(
                        1, LocalDate.now(), BigDecimal.valueOf(100.00),
                        PaymentMethod.PIX, true, null, null, null, null, null
                ),
                new OrderInstallmentRequest(
                        2, LocalDate.now().plusDays(30), BigDecimal.valueOf(35.00),
                        PaymentMethod.CREDIT_CARD, false, "VISA", "123456", null, null, null
                )
        ));

        when(orderInstallmentRepository.save(any(OrderInstallmentEntity.class))).thenAnswer(inv -> {
            OrderInstallmentEntity entity = inv.getArgument(0);
            entity.setId(500L);
            return entity;
        });

        List<OrderInstallmentEntity> result = orderPaymentService.processInstallments(request, sampleOrder);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderInstallmentRepository, times(2)).save(any(OrderInstallmentEntity.class));
        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransactionEntity.class));
    }

    @Test
    @DisplayName("Deve lançar HTTP 422 quando soma das parcelas divergir do total_final do pedido")
    void shouldThrow422WhenInstallmentsTotalMismatch() {
        OrderCloseRequest request = new OrderCloseRequest(List.of(
                new OrderInstallmentRequest(
                        1, LocalDate.now(), BigDecimal.valueOf(100.00),
                        PaymentMethod.PIX, true, null, null, null, null, null
                )
        ));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                orderPaymentService.processInstallments(request, sampleOrder)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("100.00"));
        assertTrue(ex.getReason().contains("135.00"));
        verify(orderInstallmentRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }
}
