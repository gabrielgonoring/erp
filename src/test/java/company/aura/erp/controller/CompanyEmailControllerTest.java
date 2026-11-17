package company.aura.erp.controller;

import company.aura.erp.dto.email.CompanyEmailCreateRequest;
import company.aura.erp.dto.email.CompanyEmailResponse;
import company.aura.erp.dto.email.CompanyEmailUpdateRequest;
import company.aura.erp.enums.CompanyEmailConfigStatus;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.CompanyEmailService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyEmailControllerTest {

    @Mock
    private CompanyEmailService companyEmailService;

    @InjectMocks
    private CompanyEmailController companyEmailController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void shouldCreateCompanyEmailConfigAndReturnCreated() {
        CompanyEmailCreateRequest request = new CompanyEmailCreateRequest(
                "DEFAULT",
                "contato@empresa.com",
                "Empresa",
                "resposta@empresa.com",
                CompanyEmailConfigStatus.ACTIVE
        );
        CompanyEmailResponse expected = CompanyEmailResponse.builder()
                .id(1L)
                .companyId(10L)
                .notificationType("DEFAULT")
                .fromEmail("contato@empresa.com")
                .fromName("Empresa")
                .replyTo("resposta@empresa.com")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        when(companyEmailService.create(request, 10L)).thenReturn(expected);

        ResponseEntity<CompanyEmailResponse> response = companyEmailController.create(request, userPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        verify(companyEmailService).create(request, 10L);
    }

    @Test
    void shouldListCompanyEmailConfigs() {
        Pageable pageable = PageRequest.of(0, 20);
        CompanyEmailResponse item = CompanyEmailResponse.builder()
                .id(1L)
                .companyId(10L)
                .notificationType("INVOICE_LINKED")
                .fromEmail("fiscal@empresa.com")
                .fromName("Fiscal")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();
        Page<CompanyEmailResponse> page = new PageImpl<>(List.of(item));

        when(companyEmailService.list("fiscal", "INVOICE_LINKED", CompanyEmailConfigStatus.ACTIVE, 10L, pageable))
                .thenReturn(page);

        ResponseEntity<Page<CompanyEmailResponse>> response = companyEmailController.list(
                "fiscal", "INVOICE_LINKED", CompanyEmailConfigStatus.ACTIVE, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(companyEmailService).list("fiscal", "INVOICE_LINKED", CompanyEmailConfigStatus.ACTIVE, 10L, pageable);
    }

    @Test
    void shouldFindCompanyEmailConfigById() {
        CompanyEmailResponse expected = CompanyEmailResponse.builder()
                .id(5L)
                .companyId(10L)
                .notificationType("DEFAULT")
                .fromEmail("contato@empresa.com")
                .fromName("Empresa")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        when(companyEmailService.findById(5L, 10L)).thenReturn(expected);

        ResponseEntity<CompanyEmailResponse> response = companyEmailController.findById(5L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().getId());
        verify(companyEmailService).findById(5L, 10L);
    }

    @Test
    void shouldUpdateCompanyEmailConfig() {
        CompanyEmailUpdateRequest request = new CompanyEmailUpdateRequest(
                "FORGOT_PASSWORD",
                "suporte@empresa.com",
                "Suporte",
                null,
                CompanyEmailConfigStatus.ACTIVE
        );
        CompanyEmailResponse expected = CompanyEmailResponse.builder()
                .id(5L)
                .companyId(10L)
                .notificationType("FORGOT_PASSWORD")
                .fromEmail("suporte@empresa.com")
                .fromName("Suporte")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        when(companyEmailService.update(5L, request, 10L)).thenReturn(expected);

        ResponseEntity<CompanyEmailResponse> response = companyEmailController.update(5L, request, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORGOT_PASSWORD", response.getBody().getNotificationType());
        verify(companyEmailService).update(5L, request, 10L);
    }

    @Test
    void shouldDeleteCompanyEmailConfigAndReturnNoContent() {
        ResponseEntity<Void> response = companyEmailController.delete(5L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(companyEmailService).delete(5L, 10L);
    }
}
