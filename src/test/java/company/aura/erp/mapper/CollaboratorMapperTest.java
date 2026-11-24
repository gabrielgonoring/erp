package company.aura.erp.mapper;

import company.aura.erp.dto.collaborator.CollaboratorCreateRequest;
import company.aura.erp.dto.collaborator.CollaboratorResponse;
import company.aura.erp.dto.collaborator.CollaboratorSummaryResponse;
import company.aura.erp.entity.CollaboratorEntity;
import company.aura.erp.enums.CollaboratorStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CollaboratorMapperTest {

    @Test
    @DisplayName("Deve converter CollaboratorCreateRequest para CollaboratorEntity")
    void shouldMapCreateRequestToEntity() {
        CollaboratorCreateRequest request = CollaboratorCreateRequest.builder()
                .name("Carlos Santos")
                .userId(5L)
                .cpf("111.222.333-44")
                .email("carlos@email.com")
                .phone("11988887777")
                .street("Rua A")
                .number("10")
                .city("São Paulo")
                .state("SP")
                .zipCode("01001000")
                .jobTitle("Barbeiro")
                .admissionDate(LocalDate.of(2023, 1, 10))
                .build();

        CollaboratorEntity entity = CollaboratorMapper.toEntity(request, 10L);

        assertNotNull(entity);
        assertEquals(10L, entity.getCompanyId());
        assertEquals(5L, entity.getUserId());
        assertEquals("Carlos Santos", entity.getName());
        assertEquals("111.222.333-44", entity.getCpf());
        assertEquals("carlos@email.com", entity.getEmail());
        assertEquals(CollaboratorStatus.ACTIVE.name(), entity.getStatus());
        assertEquals("Rua A", entity.getStreet());
        assertEquals("Barbeiro", entity.getJobTitle());
    }

    @Test
    @DisplayName("Deve converter CollaboratorEntity para CollaboratorResponse")
    void shouldMapEntityToResponse() {
        CollaboratorEntity entity = CollaboratorEntity.builder()
                .id(1L)
                .companyId(10L)
                .userId(5L)
                .name("Carlos Santos")
                .cpf("111.222.333-44")
                .email("carlos@email.com")
                .status("ACTIVE")
                .jobTitle("Barbeiro")
                .build();

        CollaboratorResponse response = CollaboratorMapper.toResponse(entity);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getCompanyId());
        assertEquals(5L, response.getUserId());
        assertEquals("Carlos Santos", response.getName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("Barbeiro", response.getJobTitle());
    }

    @Test
    @DisplayName("Deve converter CollaboratorEntity para CollaboratorSummaryResponse")
    void shouldMapEntityToSummaryResponse() {
        CollaboratorEntity entity = CollaboratorEntity.builder()
                .id(1L)
                .userId(5L)
                .name("Carlos Santos")
                .cpf("111.222.333-44")
                .email("carlos@email.com")
                .status("ACTIVE")
                .build();

        CollaboratorSummaryResponse summary = CollaboratorMapper.toSummaryResponse(entity);

        assertNotNull(summary);
        assertEquals(1L, summary.getId());
        assertEquals(5L, summary.getUserId());
        assertEquals("Carlos Santos", summary.getNome());
        assertEquals("Carlos Santos", summary.getName());
        assertEquals("111.222.333-44", summary.getCpf());
        assertEquals("carlos@email.com", summary.getEmail());
        assertEquals("ACTIVE", summary.getStatus());
    }

    @Test
    @DisplayName("Deve retornar null se a entidade ou request for nulo")
    void shouldHandleNullInputs() {
        assertNull(CollaboratorMapper.toEntity(null, 10L));
        assertNull(CollaboratorMapper.toResponse(null));
        assertNull(CollaboratorMapper.toSummaryResponse(null));
    }
}
