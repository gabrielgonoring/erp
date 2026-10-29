package company.aura.erp.service;

import company.aura.erp.dto.partner.PartnerCreateRequest;
import company.aura.erp.dto.partner.PartnerRequest;
import company.aura.erp.dto.partner.PartnerResponse;
import company.aura.erp.dto.partner.PartnerSummaryResponse;
import company.aura.erp.dto.partner.PartnerUpdateRequest;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.repository.CompanyRepository;
import company.aura.erp.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public PartnerResponse save(PartnerCreateRequest request, Long companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada com ID: " + companyId);
        }

        PartnerEntity partner = fromCreateRequest(request, companyId);

        PartnerEntity saved = partnerRepository.save(partner);
        return PartnerResponse.fromEntity(saved);
    }

    private static PartnerEntity fromCreateRequest(PartnerCreateRequest request, Long companyId) {
        PartnerEntity partner = PartnerEntity.builder()
                .companyId(companyId)
                .isCustomer(Boolean.TRUE.equals(request.isCustomer()))
                .isSupplier(Boolean.TRUE.equals(request.isSupplier()))
                .personType(request.personType())
                .document(request.document())
                .name(request.name())
                .indicatorIe(request.indicatorIe() != null ? request.indicatorIe() : 9)
                .stateRegistration(request.stateRegistration())
                .email(request.email())
                .phone(request.phone())
                .status(request.status())
                .street(request.street())
                .number(request.number())
                .complement(request.complement())
                .neighborhood(request.neighborhood())
                .ibgeCityCode(request.ibgeCityCode())
                .city(request.city())
                .state(request.state())
                .zipCode(request.zipCode())
                .countryCode(request.countryCode() != null ? request.countryCode() : "1058")
                .countryName(request.countryName() != null ? request.countryName() : "Brasil")
                .build();
        return partner;
    }


    @Transactional
    public PartnerResponse update(Long id, PartnerUpdateRequest request, Long companyId) {
        PartnerEntity partner = partnerRepository.findByIdAndCompanyIdAndDeletedAtIsNull(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não encontrado com ID: " + id));

        partner.setIsCustomer(request.isCustomer());
        partner.setIsSupplier(request.isSupplier());
        partner.setDocument(request.document());
        partner.setName(request.name());
       partner.setIndicatorIe(request.indicatorIe());
        partner.setStateRegistration(request.stateRegistration());
        partner.setEmail(request.email());
        partner.setPhone(request.phone());
        partner.setStatus(request.status());
        partner.setStreet(request.street());
        partner.setNumber(request.number());
        partner.setComplement(request.complement());
        partner.setNeighborhood(request.neighborhood());
        partner.setIbgeCityCode(request.ibgeCityCode());
        partner.setCity(request.city());
        partner.setState(request.state());
        partner.setZipCode(request.zipCode());
        partner.setCountryCode(request.countryCode());
        partner.setCountryName(request.countryName());

        PartnerEntity updated = partnerRepository.save(partner);
        return PartnerResponse.fromEntity(updated);
    }


    @Transactional(readOnly = true)
    public PartnerResponse findById(Long id, Long companyId) {
        PartnerEntity partner = partnerRepository.findByIdAndCompanyIdAndDeletedAtIsNull(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não encontrado com ID: " + id));

        return PartnerResponse.fromEntity(partner);
    }

    @Transactional(readOnly = true)
    public Page<PartnerSummaryResponse> listSummarized(String search, Long companyId, Pageable pageable) {
        return partnerRepository.searchByCompanyIdAndTerm(companyId, search, pageable)
                .map(PartnerSummaryResponse::fromEntity);
    }
}
