package company.aura.erp.service;

import company.aura.erp.dto.serviceorder.ServiceOrderTermCreateRequest;
import company.aura.erp.dto.serviceorder.ServiceOrderTermResponse;
import company.aura.erp.dto.serviceorder.ServiceOrderTermSummaryResponse;
import company.aura.erp.dto.serviceorder.ServiceOrderTermUpdateRequest;
import company.aura.erp.entity.ServiceOrderTermEntity;
import company.aura.erp.enums.ServiceOrderTermStatus;
import company.aura.erp.repository.ServiceOrderTermRepository;
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
class ServiceOrderTermServiceTest {

    @Mock
    private ServiceOrderTermRepository serviceOrderTermRepository;

    @InjectMocks
    private ServiceOrderTermService serviceOrderTermService;

    private final Long companyId = 10L;
    private ServiceOrderTermEntity sampleTerm;

    @BeforeEach
    void setUp() {
        sampleTerm = ServiceOrderTermEntity.builder()
                .id(1L)
                .companyId(companyId)
                .title("Termo Padrão")
                .content("Garantia de 90 dias para serviços prestados.")
                .isDefault(true)
                .status(ServiceOrderTermStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Deve listar termos de forma paginada retornando id, título e status")
    void shouldListTermsPaginated() {
        Pageable pageable = PageRequest.of(0, 20);
        ServiceOrderTermSummaryResponse summary = ServiceOrderTermSummaryResponse.builder()
                .id(1L)
                .title("Termo Padrão")
                .status(ServiceOrderTermStatus.ACTIVE)
                .build();
        Page<ServiceOrderTermSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(serviceOrderTermRepository.searchSummarized(companyId, ServiceOrderTermStatus.DELETED, "Padrão", ServiceOrderTermStatus.ACTIVE, pageable))
                .thenReturn(page);

        Page<ServiceOrderTermSummaryResponse> result = serviceOrderTermService.listSummarized("  Padrão ", ServiceOrderTermStatus.ACTIVE, companyId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals("Termo Padrão", result.getContent().get(0).getTitle());
        assertEquals(ServiceOrderTermStatus.ACTIVE, result.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("Deve buscar termo por ID")
    void shouldFindTermById() {
        when(serviceOrderTermRepository.findByIdAndCompanyIdAndStatusNot(1L, companyId, ServiceOrderTermStatus.DELETED))
                .thenReturn(Optional.of(sampleTerm));

        ServiceOrderTermResponse response = serviceOrderTermService.findById(1L, companyId);

        assertEquals(1L, response.getId());
        assertEquals("Termo Padrão", response.getTitle());
        assertEquals("Garantia de 90 dias para serviços prestados.", response.getContent());
        assertTrue(response.getIsDefault());
        assertEquals(ServiceOrderTermStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve criar novo termo padrão e desmarcar padrão anterior")
    void shouldCreateDefaultTermAndUnmarkPreviousDefault() {
        ServiceOrderTermEntity oldDefault = ServiceOrderTermEntity.builder()
                .id(99L)
                .companyId(companyId)
                .title("Antigo Padrão")
                .content("Conteúdo antigo")
                .isDefault(true)
                .status(ServiceOrderTermStatus.ACTIVE)
                .build();

        ServiceOrderTermCreateRequest request = ServiceOrderTermCreateRequest.builder()
                .title("  Novo Termo  ")
                .content("  Novo conteúdo  ")
                .isDefault(true)
                .build();

        when(serviceOrderTermRepository.findByCompanyIdAndIsDefaultTrue(companyId)).thenReturn(List.of(oldDefault));
        when(serviceOrderTermRepository.save(any(ServiceOrderTermEntity.class))).thenAnswer(inv -> {
            ServiceOrderTermEntity entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(2L);
            }
            return entity;
        });

        ServiceOrderTermResponse response = serviceOrderTermService.create(request, companyId);

        assertEquals(2L, response.getId());
        assertEquals("Novo Termo", response.getTitle());
        assertEquals("Novo conteúdo", response.getContent());
        assertTrue(response.getIsDefault());
        assertEquals(ServiceOrderTermStatus.ACTIVE, response.getStatus());
        assertFalse(oldDefault.getIsDefault());
    }

    @Test
    @DisplayName("Deve atualizar termo existente")
    void shouldUpdateTerm() {
        ServiceOrderTermUpdateRequest request = ServiceOrderTermUpdateRequest.builder()
                .title("Termo Atualizado")
                .content("Conteúdo atualizado")
                .isDefault(false)
                .status(ServiceOrderTermStatus.INACTIVE)
                .build();

        when(serviceOrderTermRepository.findByIdAndCompanyIdAndStatusNot(1L, companyId, ServiceOrderTermStatus.DELETED))
                .thenReturn(Optional.of(sampleTerm));
        when(serviceOrderTermRepository.save(any(ServiceOrderTermEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceOrderTermResponse response = serviceOrderTermService.update(1L, request, companyId);

        assertEquals("Termo Atualizado", response.getTitle());
        assertEquals("Conteúdo atualizado", response.getContent());
        assertFalse(response.getIsDefault());
        assertEquals(ServiceOrderTermStatus.INACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve apagar termo alterando status para DELETED e removendo padrão")
    void shouldDeleteTermSoftly() {
        when(serviceOrderTermRepository.findByIdAndCompanyIdAndStatusNot(1L, companyId, ServiceOrderTermStatus.DELETED))
                .thenReturn(Optional.of(sampleTerm));
        when(serviceOrderTermRepository.save(any(ServiceOrderTermEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        serviceOrderTermService.delete(1L, companyId);

        assertEquals(ServiceOrderTermStatus.DELETED, sampleTerm.getStatus());
        assertFalse(sampleTerm.getIsDefault());
        verify(serviceOrderTermRepository).save(sampleTerm);
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND ao validar termo inexistente")
    void shouldThrowNotFoundWhenTermDoesNotExist() {
        when(serviceOrderTermRepository.findByIdAndCompanyIdAndStatusNot(999L, companyId, ServiceOrderTermStatus.DELETED))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> serviceOrderTermService.validateTermExists(999L, companyId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
