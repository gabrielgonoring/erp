package company.aura.erp.service;

import company.aura.erp.dto.commission.CommissionRuleCreateRequest;
import company.aura.erp.dto.commission.CommissionRuleResponse;
import company.aura.erp.dto.commission.CommissionRuleSummaryResponse;
import company.aura.erp.dto.commission.CommissionRuleUpdateRequest;
import company.aura.erp.entity.CatalogCategoryEntity;
import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.entity.CommissionRuleEntity;
import company.aura.erp.enums.CatalogCategoryStatus;
import company.aura.erp.enums.CatalogItemStatus;
import company.aura.erp.enums.CollaboratorStatus;
import company.aura.erp.enums.CommissionRuleStatus;
import company.aura.erp.repository.CatalogCategoryRepository;
import company.aura.erp.repository.CatalogItemRepository;
import company.aura.erp.repository.CollaboratorRepository;
import company.aura.erp.repository.CommissionRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionRuleServiceTest {

    @Mock
    private CommissionRuleRepository commissionRuleRepository;
    @Mock
    private CollaboratorRepository collaboratorRepository;
    @Mock
    private CatalogCategoryRepository catalogCategoryRepository;
    @Mock
    private CatalogItemRepository catalogItemRepository;

    @InjectMocks
    private CommissionRuleService commissionRuleService;

    @Test
    @DisplayName("Deve criar regra de comissão vinculada a colaborador e categoria")
    void shouldCreateRuleForCollaboratorAndCategory() {
        CommissionRuleCreateRequest request = CommissionRuleCreateRequest.builder()
                .collaboratorId(5L)
                .categoryId(2L)
                .commissionPercentage(new BigDecimal("10.00"))
                .validFrom(LocalDate.of(2026, 1, 1))
                .validUntil(LocalDate.of(2026, 12, 31))
                .build();

        when(collaboratorRepository.findByIdAndCompanyIdAndStatusNot(5L, 10L, CollaboratorStatus.DELETED.name()))
                .thenReturn(Optional.of(CollaboratorEntity.builder().id(5L).companyId(10L).name("João").build()));
        when(catalogCategoryRepository.findByIdAndCompanyIdAndStatusNot(2L, 10L, CatalogCategoryStatus.DELETED))
                .thenReturn(Optional.of(CatalogCategoryEntity.builder().id(2L).companyId(10L).name("Serviços").build()));
        when(commissionRuleRepository.save(any(CommissionRuleEntity.class))).thenAnswer(inv -> {
            CommissionRuleEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });
        when(collaboratorRepository.findNameByIdAndCompanyId(5L, 10L)).thenReturn(Optional.of("João"));
        when(catalogCategoryRepository.findNameByIdAndCompanyId(2L, 10L)).thenReturn(Optional.of("Serviços"));

        CommissionRuleResponse response = commissionRuleService.create(request, 10L);

        assertEquals(1L, response.getId());
        assertEquals("João", response.getCollaboratorName());
        assertEquals("Serviços", response.getCategoryName());
        assertEquals(CommissionRuleStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve rejeitar regra com categoryId e catalogItemId preenchidos simultaneamente")
    void shouldRejectBothCategoryAndCatalogItem() {
        CommissionRuleCreateRequest request = CommissionRuleCreateRequest.builder()
                .categoryId(2L)
                .catalogItemId(8L)
                .commissionPercentage(new BigDecimal("10.00"))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> commissionRuleService.create(request, 10L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve rejeitar regra quando validFrom é posterior a validUntil")
    void shouldRejectInvalidValidityRange() {
        CommissionRuleCreateRequest request = CommissionRuleCreateRequest.builder()
                .commissionPercentage(new BigDecimal("5.00"))
                .validFrom(LocalDate.of(2026, 12, 31))
                .validUntil(LocalDate.of(2026, 1, 1))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> commissionRuleService.create(request, 10L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve editar regra de comissão mantendo status")
    void shouldUpdateRule() {
        CommissionRuleEntity existing = CommissionRuleEntity.builder()
                .id(1L)
                .companyId(10L)
                .commissionPercentage(new BigDecimal("5.00"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();

        CommissionRuleUpdateRequest request = CommissionRuleUpdateRequest.builder()
                .catalogItemId(8L)
                .commissionPercentage(new BigDecimal("15.00"))
                .build();

        CatalogItemEntity item = new CatalogItemEntity();
        item.setId(8L);
        item.setCompanyId(10L);
        item.setDescription("Monitor");

        when(commissionRuleRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CommissionRuleStatus.DELETED))
                .thenReturn(Optional.of(existing));
        when(catalogItemRepository.findByIdAndCompanyIdAndStatusNot(8L, 10L, CatalogItemStatus.DELETED.name()))
                .thenReturn(Optional.of(item));
        when(commissionRuleRepository.save(existing)).thenReturn(existing);
        when(catalogItemRepository.findByIdAndCompanyId(8L, 10L)).thenReturn(Optional.of(item));

        CommissionRuleResponse response = commissionRuleService.update(1L, request, 10L);

        assertEquals(8L, response.getCatalogItemId());
        assertEquals("Monitor", response.getCatalogItemDescription());
        assertEquals(new BigDecimal("15.00"), response.getCommissionPercentage());
        assertEquals(CommissionRuleStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve realizar soft delete na regra de comissão")
    void shouldSoftDeleteRule() {
        CommissionRuleEntity existing = CommissionRuleEntity.builder()
                .id(1L)
                .companyId(10L)
                .commissionPercentage(new BigDecimal("5.00"))
                .status(CommissionRuleStatus.ACTIVE)
                .build();

        when(commissionRuleRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, CommissionRuleStatus.DELETED))
                .thenReturn(Optional.of(existing));

        commissionRuleService.delete(1L, 10L);

        assertEquals(CommissionRuleStatus.DELETED, existing.getStatus());
        verify(commissionRuleRepository).save(existing);
    }

    @Test
    @DisplayName("Deve listar regras deletadas quando status pesquisado for DELETED")
    void shouldListDeletedRulesWhenStatusIsDeleted() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CommissionRuleSummaryResponse> expected = new PageImpl<>(
                List.of(CommissionRuleSummaryResponse.builder().id(2L).status(CommissionRuleStatus.DELETED).build()),
                pageable,
                1
        );

        when(commissionRuleRepository.searchSummarized(10L, null, null, null, null, CommissionRuleStatus.DELETED, pageable))
                .thenReturn(expected);

        Page<CommissionRuleSummaryResponse> result = commissionRuleService.listSummarized(
                null, null, null, null, CommissionRuleStatus.DELETED, 10L, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(CommissionRuleStatus.DELETED, result.getContent().getFirst().getStatus());
    }

    @Test
    @DisplayName("Deve listar regras paginadas incluindo deletadas quando status não for informado")
    void shouldListRules() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CommissionRuleSummaryResponse> expected = new PageImpl<>(
                List.of(
                        CommissionRuleSummaryResponse.builder().id(1L).status(CommissionRuleStatus.ACTIVE).build(),
                        CommissionRuleSummaryResponse.builder().id(2L).status(CommissionRuleStatus.DELETED).build()
                ),
                pageable,
                2
        );

        when(commissionRuleRepository.searchSummarized(10L, "João", 5L, null, null, null, pageable))
                .thenReturn(expected);

        Page<CommissionRuleSummaryResponse> result = commissionRuleService.listSummarized(
                " João ", 5L, null, null, null, 10L, pageable
        );

        assertEquals(2, result.getTotalElements());
    }
}
