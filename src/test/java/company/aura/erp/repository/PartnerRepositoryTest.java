package company.aura.erp.repository;

import company.aura.erp.dto.partner.PartnerSearchResponse;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.enums.PartnerStatus;
import company.aura.erp.enums.PersonType;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class PartnerRepositoryTest {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findActiveSimplifiedByCompanyId e searchSimplified — Devem filtrar parceiros ACTIVE por search, isCustomer, isCarrier e isSupplier paginados")
    void shouldListAndFilterActivePartnersPaginated() {
        Long companyId = 10L;
        Long otherCompanyId = 20L;

        PartnerEntity cliente = savePartner(companyId, "REF-ALICE", "Alice Silva", "12345678901", true, false, false, PartnerStatus.ACTIVE.name(), null);
        PartnerEntity fornecedor = savePartner(companyId, "CLI-EMP-02", "Empresa Beta Ltda", "98765432000188", false, false, true, PartnerStatus.ACTIVE.name(), null);
        PartnerEntity transportadora = savePartner(companyId, null, "Bruno Logística", null, null, true, null, PartnerStatus.ACTIVE.name(), null);

        // Devem ser ignorados
        savePartner(companyId, "REF-DEL", "Alice Deletada", "11122233344", true, false, false, PartnerStatus.DELETED.name(), null);
        savePartner(companyId, "REF-DEL2", "Alice DeletedAt", "55566677788", true, false, false, PartnerStatus.ACTIVE.name(), LocalDateTime.now());
        savePartner(companyId, "REF-INA", "Alice Inativa", "99988877766", true, false, false, "INACTIVE", null);
        savePartner(otherCompanyId, "REF-OUTRA", "Alice Outra Empresa", "12345678901", true, false, false, PartnerStatus.ACTIVE.name(), null);

        entityManager.flush();
        entityManager.clear();

        // Sem filtros: retorna todos os 3 ativos da empresa ordenados por name
        Page<PartnerSearchResponse> allPage = partnerRepository.findActiveSimplifiedByCompanyId(
                companyId, null, null, null, PageRequest.of(0, 2, Sort.by("name"))
        );
        assertEquals(3, allPage.getTotalElements());
        assertEquals(2, allPage.getTotalPages());
        assertEquals(2, allPage.getContent().size());
        assertEquals(cliente.getId(), allPage.getContent().get(0).id());
        assertEquals("REF-ALICE", allPage.getContent().get(0).partnerIdentifier());
        assertEquals("Alice Silva", allPage.getContent().get(0).name());
        assertEquals("12345678901", allPage.getContent().get(0).document());
        assertEquals(transportadora.getId(), allPage.getContent().get(1).id());
        assertNull(allPage.getContent().get(1).partnerIdentifier());
        assertNull(allPage.getContent().get(1).document());

        // Filtro por isCustomer = true
        Page<PartnerSearchResponse> customers = partnerRepository.findActiveSimplifiedByCompanyId(
                companyId, true, null, null, PageRequest.of(0, 20)
        );
        assertEquals(1, customers.getTotalElements());
        assertEquals(cliente.getId(), customers.getContent().get(0).id());

        // Filtro por isCarrier = true
        Page<PartnerSearchResponse> carriers = partnerRepository.findActiveSimplifiedByCompanyId(
                companyId, null, true, null, PageRequest.of(0, 20)
        );
        assertEquals(1, carriers.getTotalElements());
        assertEquals(transportadora.getId(), carriers.getContent().get(0).id());

        // Filtro por isSupplier = true e isCustomer = false (incluindo isCustomer null como false)
        Page<PartnerSearchResponse> suppliers = partnerRepository.findActiveSimplifiedByCompanyId(
                companyId, false, null, true, PageRequest.of(0, 20)
        );
        assertEquals(1, suppliers.getTotalElements());
        assertEquals(fornecedor.getId(), suppliers.getContent().get(0).id());

        // Filtro por isCustomer = false sem isSupplier (deve trazer fornecedor e transportadora com isCustomer null)
        Page<PartnerSearchResponse> nonCustomers = partnerRepository.findActiveSimplifiedByCompanyId(
                companyId, false, null, null, PageRequest.of(0, 20, Sort.by("name"))
        );
        assertEquals(2, nonCustomers.getTotalElements());

        // Filtro por nome (case-insensitive)
        Page<PartnerSearchResponse> byName = partnerRepository.searchSimplified(
                companyId, "beta", null, null, null, PageRequest.of(0, 20)
        );
        assertEquals(1, byName.getTotalElements());
        assertEquals(fornecedor.getId(), byName.getContent().get(0).id());

        // Filtro por chave de referência (partnerIdentifier)
        Page<PartnerSearchResponse> byIdentifier = partnerRepository.searchSimplified(
                companyId, "ref-alice", null, null, null, PageRequest.of(0, 20)
        );
        assertEquals(1, byIdentifier.getTotalElements());
        assertEquals(cliente.getId(), byIdentifier.getContent().get(0).id());

        // Filtro por documento
        Page<PartnerSearchResponse> byDocument = partnerRepository.searchSimplified(
                companyId, "000188", null, null, null, PageRequest.of(0, 20)
        );
        assertEquals(1, byDocument.getTotalElements());
        assertEquals(fornecedor.getId(), byDocument.getContent().get(0).id());

        // Combinação search + filtro booleano que não bate
        Page<PartnerSearchResponse> mismatch = partnerRepository.searchSimplified(
                companyId, "Alice", false, null, null, PageRequest.of(0, 20)
        );
        assertEquals(0, mismatch.getTotalElements());
    }

    private PartnerEntity savePartner(
            Long companyId,
            String partnerIdentifier,
            String name,
            String document,
            Boolean isCustomer,
            Boolean isCarrier,
            Boolean isSupplier,
            String status,
            LocalDateTime deletedAt
    ) {
        return partnerRepository.saveAndFlush(
                PartnerEntity.builder()
                        .companyId(companyId)
                        .partnerIdentifier(partnerIdentifier)
                        .name(name)
                        .document(document)
                        .personType(PersonType.PF)
                        .isCustomer(isCustomer)
                        .isCarrier(isCarrier)
                        .isSupplier(isSupplier)
                        .status(status)
                        .deletedAt(deletedAt)
                        .build()
        );
    }
}
