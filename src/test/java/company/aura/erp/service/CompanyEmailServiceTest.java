package company.aura.erp.service;

import company.aura.erp.dto.email.CompanyEmailCreateRequest;
import company.aura.erp.dto.email.CompanyEmailResponse;
import company.aura.erp.dto.email.CompanyEmailUpdateRequest;
import company.aura.erp.entity.CompanyEmailConfigEntity;
import company.aura.erp.enums.CompanyEmailConfigStatus;
import company.aura.erp.modules.email.enumerate.NotificationType;
import company.aura.erp.repository.CompanyEmailConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyEmailServiceTest {

    @Mock
    private CompanyEmailConfigRepository companyEmailConfigRepository;

    @InjectMocks
    private CompanyEmailService companyEmailService;

    @Test
    @DisplayName("Deve criar configuração padrão (DEFAULT) quando notificationType for nulo ou em branco")
    void shouldCreateDefaultConfigWhenNotificationTypeIsBlank() {
        CompanyEmailCreateRequest request = new CompanyEmailCreateRequest(
                "   ",
                " contato@empresa.com ",
                " Empresa Teste ",
                " suporte@empresa.com ",
                null
        );

        when(companyEmailConfigRepository.existsByCompanyIdAndNotificationType(10L, "DEFAULT")).thenReturn(false);
        when(companyEmailConfigRepository.save(any(CompanyEmailConfigEntity.class))).thenAnswer(invocation -> {
            CompanyEmailConfigEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        CompanyEmailResponse response = companyEmailService.create(request, 10L);

        assertEquals(1L, response.getId());
        assertEquals(10L, response.getCompanyId());
        assertEquals("DEFAULT", response.getNotificationType());
        assertEquals("contato@empresa.com", response.getFromEmail());
        assertEquals("Empresa Teste", response.getFromName());
        assertEquals("suporte@empresa.com", response.getReplyTo());
        assertEquals(CompanyEmailConfigStatus.ACTIVE, response.getStatus());

        ArgumentCaptor<CompanyEmailConfigEntity> captor = ArgumentCaptor.forClass(CompanyEmailConfigEntity.class);
        verify(companyEmailConfigRepository).save(captor.capture());
        assertEquals("DEFAULT", captor.getValue().getNotificationType());
        assertEquals(CompanyEmailConfigStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Deve criar configuração específica normalizando notificationType para maiúsculas")
    void shouldCreateSpecificNotificationTypeConfig() {
        CompanyEmailCreateRequest request = new CompanyEmailCreateRequest(
                "invoice_linked",
                "fiscal@empresa.com",
                "Fiscal Empresa",
                " ",
                CompanyEmailConfigStatus.INACTIVE
        );

        when(companyEmailConfigRepository.existsByCompanyIdAndNotificationType(10L, "INVOICE_LINKED")).thenReturn(false);
        when(companyEmailConfigRepository.save(any(CompanyEmailConfigEntity.class))).thenAnswer(invocation -> {
            CompanyEmailConfigEntity entity = invocation.getArgument(0);
            entity.setId(2L);
            return entity;
        });

        CompanyEmailResponse response = companyEmailService.create(request, 10L);

        assertEquals(2L, response.getId());
        assertEquals("INVOICE_LINKED", response.getNotificationType());
        assertNull(response.getReplyTo());
        assertEquals(CompanyEmailConfigStatus.INACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve lançar CONFLICT 409 ao criar configuração duplicada para o mesmo tipo")
    void shouldThrowConflictWhenCreatingDuplicateConfig() {
        CompanyEmailCreateRequest request = new CompanyEmailCreateRequest(
                "INSTALLMENT_NOTIFY",
                "cobranca@empresa.com",
                "Cobrança",
                null,
                CompanyEmailConfigStatus.ACTIVE
        );

        when(companyEmailConfigRepository.existsByCompanyIdAndNotificationType(10L, "INSTALLMENT_NOTIFY")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> companyEmailService.create(request, 10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(companyEmailConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST 400 ao criar com notificationType inválido")
    void shouldThrowBadRequestWhenNotificationTypeIsInvalid() {
        CompanyEmailCreateRequest request = new CompanyEmailCreateRequest(
                "UNKNOWN_TYPE",
                "contato@empresa.com",
                "Empresa",
                null,
                CompanyEmailConfigStatus.ACTIVE
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> companyEmailService.create(request, 10L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(companyEmailConfigRepository);
    }

    @Test
    @DisplayName("Deve lançar CONFLICT 409 quando companyId for nulo")
    void shouldThrowConflictWhenCompanyIdIsNull() {
        CompanyEmailCreateRequest request = new CompanyEmailCreateRequest(
                "DEFAULT",
                "contato@empresa.com",
                "Empresa",
                null,
                CompanyEmailConfigStatus.ACTIVE
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> companyEmailService.create(request, null));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(companyEmailConfigRepository);
    }

    @Test
    @DisplayName("Deve atualizar configuração de e-mail com sucesso")
    void shouldUpdateConfigSuccessfully() {
        CompanyEmailConfigEntity existing = CompanyEmailConfigEntity.builder()
                .id(5L)
                .companyId(10L)
                .notificationType("DEFAULT")
                .fromEmail("antigo@empresa.com")
                .fromName("Antigo")
                .replyTo("antigo-reply@empresa.com")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        CompanyEmailUpdateRequest request = new CompanyEmailUpdateRequest(
                "FORGOT_PASSWORD",
                "seguranca@empresa.com",
                "Segurança",
                null,
                CompanyEmailConfigStatus.INACTIVE
        );

        when(companyEmailConfigRepository.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.of(existing));
        when(companyEmailConfigRepository.existsByCompanyIdAndNotificationTypeAndIdNot(10L, "FORGOT_PASSWORD", 5L)).thenReturn(false);
        when(companyEmailConfigRepository.save(existing)).thenReturn(existing);

        CompanyEmailResponse response = companyEmailService.update(5L, request, 10L);

        assertEquals("FORGOT_PASSWORD", response.getNotificationType());
        assertEquals("seguranca@empresa.com", response.getFromEmail());
        assertEquals("Segurança", response.getFromName());
        assertNull(response.getReplyTo());
        assertEquals(CompanyEmailConfigStatus.INACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Deve lançar CONFLICT 409 ao atualizar notificationType para outro já cadastrado")
    void shouldThrowConflictWhenUpdatingToDuplicateNotificationType() {
        CompanyEmailConfigEntity existing = CompanyEmailConfigEntity.builder()
                .id(5L)
                .companyId(10L)
                .notificationType("DEFAULT")
                .fromEmail("contato@empresa.com")
                .fromName("Empresa")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        CompanyEmailUpdateRequest request = new CompanyEmailUpdateRequest(
                "INVOICE_LINKED",
                "fiscal@empresa.com",
                "Fiscal",
                null,
                CompanyEmailConfigStatus.ACTIVE
        );

        when(companyEmailConfigRepository.findByIdAndCompanyId(5L, 10L)).thenReturn(Optional.of(existing));
        when(companyEmailConfigRepository.existsByCompanyIdAndNotificationTypeAndIdNot(10L, "INVOICE_LINKED", 5L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> companyEmailService.update(5L, request, 10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(companyEmailConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar configuração existente com sucesso")
    void shouldDeleteConfigSuccessfully() {
        CompanyEmailConfigEntity existing = CompanyEmailConfigEntity.builder()
                .id(3L)
                .companyId(10L)
                .notificationType("DEFAULT")
                .fromEmail("contato@empresa.com")
                .fromName("Empresa")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        when(companyEmailConfigRepository.findByIdAndCompanyId(3L, 10L)).thenReturn(Optional.of(existing));

        companyEmailService.delete(3L, 10L);

        verify(companyEmailConfigRepository).delete(existing);
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND 404 ao buscar ou deletar configuração inexistente")
    void shouldThrowNotFoundWhenConfigDoesNotExist() {
        when(companyEmailConfigRepository.findByIdAndCompanyId(99L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException exFind = assertThrows(ResponseStatusException.class,
                () -> companyEmailService.findById(99L, 10L));
        assertEquals(HttpStatus.NOT_FOUND, exFind.getStatusCode());

        ResponseStatusException exDelete = assertThrows(ResponseStatusException.class,
                () -> companyEmailService.delete(99L, 10L));
        assertEquals(HttpStatus.NOT_FOUND, exDelete.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar configurações paginadas com filtros aplicados")
    void shouldListConfigsWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        CompanyEmailConfigEntity entity = CompanyEmailConfigEntity.builder()
                .id(1L)
                .companyId(10L)
                .notificationType("DEFAULT")
                .fromEmail("contato@empresa.com")
                .fromName("Empresa")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        when(companyEmailConfigRepository.searchByCompanyId(
                10L, "contato", "DEFAULT", CompanyEmailConfigStatus.ACTIVE, pageable
        )).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<CompanyEmailResponse> page = companyEmailService.list(
                " contato ", "default", CompanyEmailConfigStatus.ACTIVE, 10L, pageable
        );

        assertEquals(1, page.getTotalElements());
        assertEquals("DEFAULT", page.getContent().get(0).getNotificationType());
        assertEquals("contato@empresa.com", page.getContent().get(0).getFromEmail());
    }

    @Test
    @DisplayName("Deve resolver configuração ativa priorizando tipo específico e retornando primeiro resultado")
    void shouldResolveActiveConfigPrioritizingSpecificOverDefault() {
        CompanyEmailConfigEntity specific = CompanyEmailConfigEntity.builder()
                .id(10L)
                .companyId(1L)
                .notificationType("INVOICE_LINKED")
                .fromEmail("fiscal@empresa.com")
                .fromName("Fiscal")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        CompanyEmailConfigEntity defaultCfg = CompanyEmailConfigEntity.builder()
                .id(11L)
                .companyId(1L)
                .notificationType("DEFAULT")
                .fromEmail("contato@empresa.com")
                .fromName("Contato")
                .status(CompanyEmailConfigStatus.ACTIVE)
                .build();

        when(companyEmailConfigRepository.findActiveConfigsForNotification(
                1L, "INVOICE_LINKED", CompanyEmailConfigStatus.ACTIVE
        )).thenReturn(List.of(specific, defaultCfg));

        Optional<CompanyEmailConfigEntity> resolved = companyEmailService.resolveActiveConfig(1L, NotificationType.INVOICE_LINKED);

        assertTrue(resolved.isPresent());
        assertEquals("INVOICE_LINKED", resolved.get().getNotificationType());
    }

    @Test
    @DisplayName("Deve retornar Optional.empty quando não houver configuração ou parâmetros forem nulos")
    void shouldReturnEmptyWhenNoConfigOrNullParams() {
        assertTrue(companyEmailService.resolveActiveConfig(null, NotificationType.FORGOT_PASSWORD).isEmpty());
        assertTrue(companyEmailService.resolveActiveConfig(1L, null).isEmpty());
        verifyNoInteractions(companyEmailConfigRepository);

        when(companyEmailConfigRepository.findActiveConfigsForNotification(
                1L, "FORGOT_PASSWORD", CompanyEmailConfigStatus.ACTIVE
        )).thenReturn(Collections.emptyList());

        assertTrue(companyEmailService.resolveActiveConfig(1L, NotificationType.FORGOT_PASSWORD).isEmpty());
    }
}
