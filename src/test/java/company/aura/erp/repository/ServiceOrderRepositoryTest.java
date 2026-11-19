package company.aura.erp.repository;

import company.aura.erp.dto.serviceorder.ServiceOrderSummaryResponse;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.PersonType;
import company.aura.erp.enums.ServiceOrderItemStatus;
import company.aura.erp.enums.ServiceOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ServiceOrderRepositoryTest {

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private ServiceOrderItemRepository serviceOrderItemRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Test
    @DisplayName("Deve persistir OS e itens e executar searchSummarized com filtros")
    void shouldPersistAndSearchSummarizedServiceOrders() {
        PartnerEntity partner = partnerRepository.save(PartnerEntity.builder()
                .companyId(1L)
                .personType(PersonType.PF)
                .document("12345678901")
                .name("Maria Silva")
                .isCustomer(true)
                .build());

        CollaboratorEntity technician = collaboratorRepository.save(CollaboratorEntity.builder()
                .companyId(1L)
                .name("Carlos Técnico")
                .status("ACTIVE")
                .build());

        ServiceOrderEntity osWithTech = serviceOrderRepository.save(ServiceOrderEntity.builder()
                .companyId(1L)
                .orderNumber(1)
                .partnerId(partner.getId())
                .technicianId(technician.getId())
                .status(ServiceOrderStatus.TRIAGE)
                .objectTitle("MacBook Pro 14")
                .objectIdentifier("C02XYZ")
                .reportedIssue("Tela piscando")
                .servicesAmount(BigDecimal.valueOf(200.00))
                .productsAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(200.00))
                .entryAt(LocalDateTime.of(2026, 11, 10, 10, 0))
                .build());

        ServiceOrderEntity osWithoutTech = serviceOrderRepository.save(ServiceOrderEntity.builder()
                .companyId(1L)
                .orderNumber(2)
                .partnerId(partner.getId())
                .status(ServiceOrderStatus.IN_PROGRESS)
                .objectTitle("iPhone 15")
                .objectIdentifier("IMEI999")
                .reportedIssue("Bateria viciada")
                .entryAt(LocalDateTime.of(2026, 11, 15, 14, 0))
                .build());

        ServiceOrderItemEntity savedItem = serviceOrderItemRepository.save(ServiceOrderItemEntity.builder()
                .serviceOrderId(osWithTech.getId())
                .itemType(CatalogItemType.SERVICE)
                .status(ServiceOrderItemStatus.PENDING)
                .description("Análise técnica")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(200.00))
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(200.00))
                .professionalId(technician.getId())
                .stockReserved(false)
                .build());

        assertEquals(ServiceOrderItemStatus.PENDING, savedItem.getStatus());
        assertEquals(2, serviceOrderRepository.findMaxOrderNumberByCompanyId(1L));
        assertEquals(1, serviceOrderItemRepository.findByServiceOrderIdAndStatusNotIn(osWithTech.getId()).size());

        PageRequest defaultSortPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ServiceOrderSummaryResponse> all = serviceOrderRepository.searchSummarized(
                1L, null, false, false, false, null, null,
                false, List.of(ServiceOrderStatus.TRIAGE),
                null, null, null, defaultSortPageable
        );
        assertEquals(2, all.getTotalElements());

        Page<ServiceOrderSummaryResponse> filtered = serviceOrderRepository.searchSummarized(
                1L, "MacBook", true, false, false,
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 12, 23, 59),
                true, List.of(ServiceOrderStatus.TRIAGE),
                partner.getId(), technician.getId(), "C02", defaultSortPageable
        );
        assertEquals(1, filtered.getTotalElements());
        ServiceOrderSummaryResponse summary = filtered.getContent().get(0);
        assertEquals(osWithTech.getId(), summary.getId());
        assertEquals("Maria Silva", summary.getCustomerName());
        assertEquals("Carlos Técnico", summary.getTechnicianName());
        assertEquals("C02XYZ", summary.getObjectIdentifier());

        assertNotNull(osWithoutTech.getId());
    }
}
