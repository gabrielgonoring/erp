package company.aura.erp.service;

import company.aura.erp.dto.partner.PartnerResponse;
import company.aura.erp.dto.partner.PartnerSummaryResponse;
import company.aura.erp.dto.partner.PartnerUpdateRequest;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.enums.PartnerStatus;
import company.aura.erp.enums.PersonType;
import company.aura.erp.repository.CompanyRepository;
import company.aura.erp.repository.PartnerRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private PartnerService partnerService;

    private PartnerEntity activePartner;

    @BeforeEach
    void setUp() {
        activePartner = PartnerEntity.builder()
                .id(1L)
                .companyId(10L)
                .name("Empresa Parceira")
                .document("12345678000199")
                .personType(PersonType.PJ)
                .email("contato@parceira.com")
                .street("Rua das Flores")
                .number("100")
                .neighborhood("Centro")
                .ibgeCityCode("3550308")
                .city("São Paulo")
                .state("SP")
                .zipCode("01001000")
                .status(PartnerStatus.ACTIVE.name())
                .build();
    }

    @Test
    void shouldDeletePartnerByChangingStatusToDeleted() {
        when(partnerRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, PartnerStatus.DELETED.name()))
                .thenReturn(Optional.of(activePartner));
        when(partnerRepository.save(any(PartnerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        partnerService.delete(1L, 10L);

        assertEquals(PartnerStatus.DELETED.name(), activePartner.getStatus());
        verify(partnerRepository).save(activePartner);
        verify(partnerRepository, never()).delete(any());
        verify(partnerRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowNotFoundWhenDeletingPartnerThatDoesNotExistOrBelongsToAnotherCompany() {
        when(partnerRepository.findByIdAndCompanyIdAndStatusNot(99L, 10L, PartnerStatus.DELETED.name()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                partnerService.delete(99L, 10L)
        );

        assertTrue(ex.getMessage().contains("Parceiro não encontrado"));
        verify(partnerRepository, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenFindingByIdPartnerThatIsDeleted() {
        when(partnerRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, PartnerStatus.DELETED.name()))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                partnerService.findById(1L, 10L)
        );
    }

    @Test
    void shouldFindActivePartnerById() {
        when(partnerRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, PartnerStatus.DELETED.name()))
                .thenReturn(Optional.of(activePartner));

        PartnerResponse response = partnerService.findById(1L, 10L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Empresa Parceira", response.name());
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingDeletedPartner() {
        when(partnerRepository.findByIdAndCompanyIdAndStatusNot(1L, 10L, PartnerStatus.DELETED.name()))
                .thenReturn(Optional.empty());

        PartnerUpdateRequest updateRequest = new PartnerUpdateRequest(
                true, false, "12345678000199", "Nome Atualizado",
                9, null, "email@teste.com", "1199999999",
                "Rua X", "12", null, "Bairro", "3550308", "São Paulo", "SP", "01001000", "1058", "Brasil"
        );

        assertThrows(ResponseStatusException.class, () ->
                partnerService.update(1L, updateRequest, 10L)
        );
    }

    @Test
    void shouldListSummarizedPartnersCallingRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PartnerEntity> partnerPage = new PageImpl<>(List.of(activePartner));

        when(partnerRepository.searchByCompanyIdAndTerm(eq(10L), eq("São Paulo"), eq(pageable)))
                .thenReturn(partnerPage);

        Page<PartnerSummaryResponse> result = partnerService.listSummarized("São Paulo", 10L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("São Paulo", result.getContent().get(0).city());
    }
}
