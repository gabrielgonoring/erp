package company.aura.erp.controller;

import company.aura.erp.dto.company.CompanyTermCreateRequest;
import company.aura.erp.dto.company.CompanyTermResponse;
import company.aura.erp.dto.company.CompanyTermSummaryResponse;
import company.aura.erp.dto.company.CompanyTermUpdateRequest;
import company.aura.erp.enums.CompanyTermStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CompanyTermService;
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
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyTermsControllerTest {

    @Mock
    private CompanyTermService companyTermService;

    @InjectMocks
    private CompanyTermsController companyTermsController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("admin@empresa.com")
                .name("Admin")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("GET /api/company/terms — Deve listar termos paginados")
    void shouldListCompanyTerms() {
        Pageable pageable = PageRequest.of(0, 20);
        CompanyTermSummaryResponse summary = CompanyTermSummaryResponse.builder()
                .id(1L)
                .title("Termo de Garantia")
                .status(CompanyTermStatus.ACTIVE)
                .isDefault(true)
                .build();
        Page<CompanyTermSummaryResponse> page = new PageImpl<>(List.of(summary), pageable, 1);

        when(companyTermService.listSummarized("Garantia", CompanyTermStatus.ACTIVE, 10L, pageable))
                .thenReturn(page);

        ResponseEntity<Page<CompanyTermSummaryResponse>> response = companyTermsController.list(
                "Garantia", CompanyTermStatus.ACTIVE, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(companyTermService).listSummarized("Garantia", CompanyTermStatus.ACTIVE, 10L, pageable);
    }

    @Test
    @DisplayName("GET /api/company/terms/{id} — Deve retornar termo por ID")
    void shouldFindCompanyTermById() {
        CompanyTermResponse termResponse = CompanyTermResponse.builder()
                .id(1L)
                .companyId(10L)
                .title("Termo de Garantia")
                .content("Garantia de 90 dias.")
                .isDefault(true)
                .status(CompanyTermStatus.ACTIVE)
                .build();

        when(companyTermService.findById(1L, 10L)).thenReturn(termResponse);

        ResponseEntity<CompanyTermResponse> response = companyTermsController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(termResponse, response.getBody());
        verify(companyTermService).findById(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/company/terms — Deve criar termo e retornar HTTP 201")
    void shouldCreateCompanyTerm() {
        CompanyTermCreateRequest request = CompanyTermCreateRequest.builder()
                .title("Termo de Garantia")
                .content("Garantia de 90 dias.")
                .isDefault(true)
                .build();

        CompanyTermResponse termResponse = CompanyTermResponse.builder()
                .id(1L)
                .companyId(10L)
                .title("Termo de Garantia")
                .content("Garantia de 90 dias.")
                .isDefault(true)
                .status(CompanyTermStatus.ACTIVE)
                .build();

        when(companyTermService.create(request, 10L)).thenReturn(termResponse);

        ResponseEntity<CompanyTermResponse> response = companyTermsController.create(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(termResponse, response.getBody());
        verify(companyTermService).create(request, 10L);
    }

    @Test
    @DisplayName("PUT /api/company/terms/{id} — Deve atualizar termo e retornar HTTP 200")
    void shouldUpdateCompanyTerm() {
        CompanyTermUpdateRequest request = CompanyTermUpdateRequest.builder()
                .title("Termo Atualizado")
                .content("Conteúdo atualizado.")
                .isDefault(false)
                .build();

        CompanyTermResponse termResponse = CompanyTermResponse.builder()
                .id(1L)
                .companyId(10L)
                .title("Termo Atualizado")
                .content("Conteúdo atualizado.")
                .isDefault(false)
                .status(CompanyTermStatus.ACTIVE)
                .build();

        when(companyTermService.update(1L, request, 10L)).thenReturn(termResponse);

        ResponseEntity<CompanyTermResponse> response = companyTermsController.update(1L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(termResponse, response.getBody());
        verify(companyTermService).update(1L, request, 10L);
    }

    @Test
    @DisplayName("DELETE /api/company/terms/{id} — Deve apagar termo e retornar HTTP 204")
    void shouldDeleteCompanyTerm() {
        ResponseEntity<Void> response = companyTermsController.delete(1L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(companyTermService).delete(1L, 10L);
    }
}
