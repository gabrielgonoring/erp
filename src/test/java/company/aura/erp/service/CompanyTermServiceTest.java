package company.aura.erp.service;

import company.aura.erp.dto.company.CompanyTermCreateRequest;
import company.aura.erp.dto.company.CompanyTermResponse;
import company.aura.erp.dto.company.CompanyTermSummaryResponse;
import company.aura.erp.dto.company.CompanyTermUpdateRequest;
import company.aura.erp.entity.CompanyTermEntity;
import company.aura.erp.enums.CompanyTermStatus;
import company.aura.erp.repository.CompanyTermRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyTermServiceTest {

    @Mock
    private CompanyTermRepository companyTermRepository;

    @InjectMocks
    private CompanyTermService companyTermService;

    private final Long companyId = 10L;
    private CompanyTermEntity sampleTerm;

    @BeforeEach
    void setUp() {
        sampleTerm = CompanyTermEntity.builder()
                .id(1L)
                .companyId(companyId)
                .title("Termo Padrão")
                .content("Garantia de 90 dias para serviços prestados.")
                .isDefault(true)
                .status(CompanyTermStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Deve listar termos de forma paginada retornando id, título e status")
    void shouldListTermsPaginated() {
        Pageable pageable = PageRequest.of(0, 20);
        CompanyTermSummaryResponse summary = CompanyTermSummaryResponse.builder()
                .id(1L)
                .title("Termo Padrão")
                .status(CompanyTermStatus.ACTIVE)
                .isDefault(true)
                .build();
        Page<CompanyTermSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(companyTermRepository.searchSummarized(companyId, CompanyTermStatus.DELETED, "Padrão", CompanyTermStatus.ACTIVE, pageable))
                .thenReturn(page);

        Page<CompanyTermSummaryResponse> result = companyTermService.listSummarized("  Padrão ", CompanyTermStatus.ACTIVE, companyId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals("Termo Padrão", result.getContent().get(0).getTitle());
        assertEquals(CompanyTermStatus.ACTIVE, result.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("Deve buscar termo por ID")
    void shouldFindTermById() {
        when(companyTermRepository.findByIdAndCompanyIdAndStatusNot(1L, companyId, CompanyTermStatus.DELETED))
                .thenReturn(Optional.of(sampleTerm));

        CompanyTermResponse response = companyTermService.findById(1L, companyId);

        assertEquals(1L, response.getId());
        assertEquals("Termo Padrão", response.getTitle());
        assertEquals("Garantia de 90 dias para serviços prestados.", response.getContent());
        assertTrue(response.getIsDefault());
        assertEquals(CompanyTermStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve criar novo termo padrão e desmarcar padrão anterior")
    void shouldCreateDefaultTermAndUnmarkPreviousDefault() {
        CompanyTermEntity oldDefault = CompanyTermEntity.builder()
                .id(99L)
                .companyId(companyId)
                .title("Antigo Padrão")
                .content("Conteúdo antigo")
                .isDefault(true)
                .status(CompanyTermStatus.ACTIVE)
                .build();

        CompanyTermCreateRequest request = CompanyTermCreateRequest.builder()
                .title("  Novo Termo  ")
                .content("  Novo conteúdo  ")
                .isDefault(true)
                .build();

        when(companyTermRepository.findByCompanyIdAndIsDefaultTrue(companyId)).thenReturn(List.of(oldDefault));
        when(companyTermRepository.save(any(CompanyTermEntity.class))).thenAnswer(inv -> {
            CompanyTermEntity entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(2L);
            }
            return entity;
        });

        CompanyTermResponse response = companyTermService.create(request, companyId);

        assertEquals(2L, response.getId());
        assertEquals("Novo Termo", response.getTitle());
        assertEquals("Novo conteúdo", response.getContent());
        assertTrue(response.getIsDefault());
        assertEquals(CompanyTermStatus.ACTIVE, response.getStatus());
        assertFalse(oldDefault.getIsDefault());
    }

    @Test
    @DisplayName("Deve atualizar termo existente")
    void shouldUpdateTerm() {
        CompanyTermUpdateRequest request = CompanyTermUpdateRequest.builder()
                .title("Termo Atualizado")
                .content("Conteúdo atualizado")
                .isDefault(false)
                .build();

        when(companyTermRepository.findByIdAndCompanyIdAndStatusNot(1L, companyId, CompanyTermStatus.DELETED))
                .thenReturn(Optional.of(sampleTerm));
        when(companyTermRepository.save(any(CompanyTermEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanyTermResponse response = companyTermService.update(1L, request, companyId);

        assertEquals("Termo Atualizado", response.getTitle());
        assertEquals("Conteúdo atualizado", response.getContent());
        assertFalse(response.getIsDefault());
        assertEquals(CompanyTermStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve apagar termo alterando status para DELETED e removendo padrão")
    void shouldDeleteTermSoftly() {
        when(companyTermRepository.findByIdAndCompanyIdAndStatusNot(1L, companyId, CompanyTermStatus.DELETED))
                .thenReturn(Optional.of(sampleTerm));
        when(companyTermRepository.save(any(CompanyTermEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        companyTermService.delete(1L, companyId);

        assertEquals(CompanyTermStatus.DELETED, sampleTerm.getStatus());
        assertFalse(sampleTerm.getIsDefault());
        verify(companyTermRepository).save(sampleTerm);
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND ao validar termo inexistente")
    void shouldThrowNotFoundWhenTermDoesNotExist() {
        when(companyTermRepository.findByIdAndCompanyIdAndStatusNot(999L, companyId, CompanyTermStatus.DELETED))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> companyTermService.validateTermExists(999L, companyId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
