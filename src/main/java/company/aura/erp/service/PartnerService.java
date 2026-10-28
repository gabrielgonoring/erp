package company.aura.erp.service;

import company.aura.erp.dto.partner.PartnerRequest;
import company.aura.erp.dto.partner.PartnerResponse;
import company.aura.erp.dto.partner.PartnerSummaryResponse;
import company.aura.erp.entity.CompanyEntity;
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
    public PartnerResponse save(PartnerRequest request, Long companyId) {
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada com ID: " + companyId));

        PartnerEntity partner = PartnerEntity.builder()
                .company(company)
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

        PartnerEntity saved = partnerRepository.save(partner);
        return PartnerResponse.fromEntity(saved);
    }

    @Transactional
    public PartnerResponse update(Long id, PartnerRequest request, Long companyId) {
        PartnerEntity partner = partnerRepository.findByIdAndCompanyIdAndDeletedAtIsNull(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não encontrado com ID: " + id));

        if (request.isCustomer() != null) partner.setIsCustomer(request.isCustomer());
        if (request.isSupplier() != null) partner.setIsSupplier(request.isSupplier());
        if (request.personType() != null) partner.setPersonType(request.personType());
        if (request.document() != null) partner.setDocument(request.document());
        if (request.name() != null) partner.setName(request.name());
        if (request.indicatorIe() != null) partner.setIndicatorIe(request.indicatorIe());
        if (request.stateRegistration() != null) partner.setStateRegistration(request.stateRegistration());
        if (request.email() != null) partner.setEmail(request.email());
        if (request.phone() != null) partner.setPhone(request.phone());
        if (request.status() != null) partner.setStatus(request.status());
        if (request.street() != null) partner.setStreet(request.street());
        if (request.number() != null) partner.setNumber(request.number());
        if (request.complement() != null) partner.setComplement(request.complement());
        if (request.neighborhood() != null) partner.setNeighborhood(request.neighborhood());
        if (request.ibgeCityCode() != null) partner.setIbgeCityCode(request.ibgeCityCode());
        if (request.city() != null) partner.setCity(request.city());
        if (request.state() != null) partner.setState(request.state());
        if (request.zipCode() != null) partner.setZipCode(request.zipCode());
        if (request.countryCode() != null) partner.setCountryCode(request.countryCode());
        if (request.countryName() != null) partner.setCountryName(request.countryName());

        PartnerEntity updated = partnerRepository.save(partner);
        return PartnerResponse.fromEntity(updated);
    }


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
