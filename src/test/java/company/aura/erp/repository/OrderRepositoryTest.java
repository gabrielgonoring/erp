package company.aura.erp.repository;

import company.aura.erp.dto.order.OrderSummaryResponse;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.PersonType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findSimilarOrders — Deve retornar pedidos da mesma empresa, mesmo cliente, mesmo valor, na janela de -2 a +2 dias e não cancelados")
    void shouldFindSimilarOrdersMatchingCompanyPartnerAmountAndDateWindow() {
        Long companyId = 1L;
        Long otherCompanyId = 2L;
        LocalDate targetDate = LocalDate.of(2026, 11, 20);

        PartnerEntity partner = savePartner(companyId, "Cliente Alvo", "12345678901");
        PartnerEntity otherPartner = savePartner(companyId, "Outro Cliente", "98765432100");
        PartnerEntity otherCompanyPartner = savePartner(otherCompanyId, "Cliente Outra Empresa", "11122233344");

        OrderEntity currentOrder = saveOrder(
                companyId, partner.getId(), OrderStatus.OPEN, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 20, 8, 0), null
        );
        OrderEntity similarTwoDaysAfter = saveOrder(
                companyId, partner.getId(), OrderStatus.COMPLETED, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 10, 9, 0), LocalDateTime.of(2026, 11, 22, 23, 59, 59)
        );
        OrderEntity similarSameDay = saveOrder(
                companyId, partner.getId(), OrderStatus.COMPLETED, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 10, 9, 0), LocalDateTime.of(2026, 11, 20, 14, 30)
        );
        OrderEntity similarTwoDaysBeforeOpen = saveOrder(
                companyId, partner.getId(), OrderStatus.OPEN, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 18, 0, 0, 0), null
        );

        // Devem ser ignorados
        saveOrder(companyId, partner.getId(), OrderStatus.CANCELED, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 20, 12, 0), LocalDateTime.of(2026, 11, 20, 12, 5));
        saveOrder(companyId, partner.getId(), OrderStatus.COMPLETED, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 17, 23, 59, 59), LocalDateTime.of(2026, 11, 17, 23, 59, 59));
        saveOrder(companyId, partner.getId(), OrderStatus.COMPLETED, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 23, 0, 0, 0), LocalDateTime.of(2026, 11, 23, 0, 0, 0));
        saveOrder(companyId, partner.getId(), OrderStatus.COMPLETED, new BigDecimal("149.99"),
                LocalDateTime.of(2026, 11, 20, 16, 0), LocalDateTime.of(2026, 11, 20, 16, 0));
        saveOrder(companyId, otherPartner.getId(), OrderStatus.COMPLETED, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 20, 16, 0), LocalDateTime.of(2026, 11, 20, 16, 0));
        saveOrder(otherCompanyId, otherCompanyPartner.getId(), OrderStatus.COMPLETED, new BigDecimal("150.00"),
                LocalDateTime.of(2026, 11, 20, 16, 0), LocalDateTime.of(2026, 11, 20, 16, 0));

        entityManager.flush();
        entityManager.clear();

        List<OrderSummaryResponse> result = orderRepository.findSimilarOrders(
                companyId,
                currentOrder.getId(),
                partner.getId(),
                OrderStatus.CANCELED,
                new BigDecimal("150.00"),
                targetDate.minusDays(2).atStartOfDay(),
                targetDate.plusDays(3).atStartOfDay()
        );

        assertEquals(3, result.size());
        assertEquals(similarTwoDaysAfter.getId(), result.get(0).getId());
        assertEquals(similarSameDay.getId(), result.get(1).getId());
        assertEquals("Cliente Alvo", result.get(1).getCustomerName());
        assertEquals("12345678901", result.get(1).getCustomerDocument());
        assertEquals(similarTwoDaysBeforeOpen.getId(), result.get(2).getId());
    }

    private PartnerEntity savePartner(Long companyId, String name, String document) {
        return partnerRepository.saveAndFlush(
                PartnerEntity.builder()
                        .companyId(companyId)
                        .personType(PersonType.PF)
                        .name(name)
                        .document(document)
                        .isCustomer(true)
                        .build()
        );
    }

    private OrderEntity saveOrder(
            Long companyId,
            Long partnerId,
            OrderStatus status,
            BigDecimal totalAmount,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        return orderRepository.saveAndFlush(
                OrderEntity.builder()
                        .companyId(companyId)
                        .partnerId(partnerId)
                        .status(status)
                        .totalAmount(totalAmount)
                        .createdAt(createdAt)
                        .completedAt(completedAt)
                        .build()
        );
    }
}
