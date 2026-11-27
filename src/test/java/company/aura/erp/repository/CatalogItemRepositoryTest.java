package company.aura.erp.repository;

import company.aura.erp.dto.catalog.CatalogItemSearchResponse;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.enums.CatalogItemStatus;
import company.aura.erp.enums.CatalogItemType;
import jakarta.persistence.EntityManager;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class CatalogItemRepositoryTest {

    @Autowired
    private CatalogItemRepository catalogItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findActiveSimplifiedByCompanyId e searchSimplified — Devem retornar itens ACTIVE paginados filtrando por descrição, id e internalCode")
    void shouldListAndFilterActiveCatalogItemsPaginated() {
        Long companyId = 10L;
        Long otherCompanyId = 20L;

        CatalogItemEntity monitor = saveItem(companyId, "EXT-MON-01", "Monitor Gamer 27", new BigDecimal("1500.00"), "UN", CatalogItemStatus.ACTIVE.name(), null);
        CatalogItemEntity service = saveItem(companyId, "SRV-CONS", "Consultoria Técnica", new BigDecimal("250.00"), "HR", CatalogItemStatus.ACTIVE.name(), null);
        CatalogItemEntity noCode = saveItem(companyId, null, "Cabo HDMI 2m", new BigDecimal("45.90"), "UN", CatalogItemStatus.ACTIVE.name(), null);

        // Devem ser ignorados
        saveItem(companyId, "DEL-01", "Monitor Antigo", new BigDecimal("500.00"), "UN", CatalogItemStatus.DELETED.name(), null);
        saveItem(companyId, "DEL-02", "Monitor Deletado", new BigDecimal("600.00"), "UN", CatalogItemStatus.ACTIVE.name(), LocalDateTime.now());
        saveItem(companyId, "INA-01", "Monitor Inativo", new BigDecimal("700.00"), "UN", "INACTIVE", null);
        saveItem(otherCompanyId, "EXT-MON-99", "Monitor Outra Empresa", new BigDecimal("999.00"), "UN", CatalogItemStatus.ACTIVE.name(), null);

        entityManager.flush();
        entityManager.clear();

        // Sem filtro: query dedicada findActiveSimplifiedByCompanyId
        Page<CatalogItemSearchResponse> allPage = catalogItemRepository.findActiveSimplifiedByCompanyId(
                companyId, PageRequest.of(0, 2, Sort.by("description"))
        );
        assertEquals(3, allPage.getTotalElements());
        assertEquals(2, allPage.getTotalPages());
        assertEquals(2, allPage.getContent().size());
        assertEquals(noCode.getId(), allPage.getContent().get(0).id());
        assertNull(allPage.getContent().get(0).internalCode());
        assertEquals(new BigDecimal("45.90"), allPage.getContent().get(0).unitPrice());
        assertEquals("Cabo HDMI 2m", allPage.getContent().get(0).description());
        assertEquals("UN", allPage.getContent().get(0).unitOfMeasure());

        // Filtro por descrição (case-insensitive)
        Page<CatalogItemSearchResponse> byDescription = catalogItemRepository.searchSimplified(
                companyId, "monitor", PageRequest.of(0, 20)
        );
        assertEquals(1, byDescription.getTotalElements());
        assertEquals(monitor.getId(), byDescription.getContent().get(0).id());
        assertEquals("EXT-MON-01", byDescription.getContent().get(0).internalCode());
        assertEquals(new BigDecimal("1500.00"), byDescription.getContent().get(0).unitPrice());
        assertEquals("Monitor Gamer 27", byDescription.getContent().get(0).description());
        assertEquals("UN", byDescription.getContent().get(0).unitOfMeasure());

        // Filtro por internalCode / id externo (case-insensitive)
        Page<CatalogItemSearchResponse> byExternalId = catalogItemRepository.searchSimplified(
                companyId, "srv-cons", PageRequest.of(0, 20)
        );
        assertEquals(1, byExternalId.getTotalElements());
        assertEquals(service.getId(), byExternalId.getContent().get(0).id());
        assertEquals("HR", byExternalId.getContent().get(0).unitOfMeasure());

        // Filtro por id interno
        Page<CatalogItemSearchResponse> byId = catalogItemRepository.searchSimplified(
                companyId, String.valueOf(noCode.getId()), PageRequest.of(0, 20)
        );
        assertEquals(1, byId.getTotalElements());
        assertEquals(noCode.getId(), byId.getContent().get(0).id());
    }

    private CatalogItemEntity saveItem(
            Long companyId,
            String internalCode,
            String description,
            BigDecimal unitPrice,
            String unitOfMeasure,
            String status,
            LocalDateTime deletedAt
    ) {
        return catalogItemRepository.saveAndFlush(
                CatalogItemEntity.builder()
                        .companyId(companyId)
                        .type(CatalogItemType.PHYSICAL)
                        .internalCode(internalCode)
                        .description(description)
                        .unitPrice(unitPrice)
                        .unitOfMeasure(unitOfMeasure)
                        .status(status)
                        .deletedAt(deletedAt)
                        .build()
        );
    }
}
