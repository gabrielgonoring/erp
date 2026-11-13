package company.aura.erp.service;

import company.aura.erp.dto.partner.PartnerCreateRequest;
import company.aura.erp.dto.partner.PartnerResponse;
import company.aura.erp.dto.partner.PartnerSummaryResponse;
import company.aura.erp.dto.partner.PartnerUpdateRequest;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.enums.PartnerStatus;
import company.aura.erp.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;

    @Transactional
    public PartnerResponse save(PartnerCreateRequest request, Long companyId) {
        log.info("save() - START - companyId={}, document={}, name={}", companyId, request != null ? request.document() : null, request != null ? request.name() : null);

        PartnerEntity partner = toEntity(request, companyId);
        PartnerEntity saved = partnerRepository.save(partner);
        PartnerResponse response = PartnerResponse.fromEntity(saved);

        log.info("save() - END - id={}, companyId={}", response.id(), companyId);
        return response;
    }

    private PartnerEntity toEntity(PartnerCreateRequest request, Long companyId) {
        return PartnerEntity.builder()
                .companyId(companyId)
                .partnerIdentifier(request.partnerIdentifier())
                .isCustomer(request.isCustomer())
                .isSupplier(request.isSupplier())
                .isCarrier(request.isCarrier() != null ? request.isCarrier() : false)
                .personType(request.personType())
                .document(request.document())
                .name(request.name())
                .indicatorIe(request.indicatorIe())
                .stateRegistration(request.stateRegistration())
                .email(request.email())
                .phone(request.phone())
                .status(PartnerStatus.ACTIVE.name())
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
    }

    @Transactional
    public PartnerResponse update(Long id, PartnerUpdateRequest request, Long companyId) {
        log.info("update() - START - id={}, companyId={}, name={}", id, companyId, request != null ? request.name() : null);

        PartnerEntity partner = partnerRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, PartnerStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não encontrado com ID: " + id));

        partner.setPartnerIdentifier(request.partnerIdentifier());
        partner.setIsCustomer(request.isCustomer());
        partner.setIsSupplier(request.isSupplier());
        if (request.isCarrier() != null) {
            partner.setIsCarrier(request.isCarrier());
        }
        partner.setDocument(request.document());
        partner.setName(request.name());
        partner.setIndicatorIe(request.indicatorIe());
        partner.setStateRegistration(request.stateRegistration());
        partner.setEmail(request.email());
        partner.setPhone(request.phone());
        partner.setStatus(PartnerStatus.ACTIVE.name());
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
        PartnerResponse response = PartnerResponse.fromEntity(updated);

        log.info("update() - END - id={}, companyId={}", response.id(), companyId);
        return response;
    }

    @Transactional(readOnly = true)
    public PartnerResponse findById(Long id, Long companyId) {
        log.info("findById() - START - id={}, companyId={}", id, companyId);

        PartnerEntity partner = partnerRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, PartnerStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não encontrado com ID: " + id));

        PartnerResponse response = PartnerResponse.fromEntity(partner);

        log.info("findById() - END - id={}, companyId={}", response.id(), companyId);
        return response;
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        log.info("delete() - START - id={}, companyId={}", id, companyId);

        PartnerEntity partner = partnerRepository.findByIdAndCompanyIdAndStatusNot(id, companyId, PartnerStatus.DELETED.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parceiro não encontrado com ID: " + id));

        partner.setStatus(PartnerStatus.DELETED.name());
        partnerRepository.save(partner);

        log.info("delete() - END - id={}, companyId={}, status={}", id, companyId, partner.getStatus());
    }

    @Transactional(readOnly = true)
    public Page<PartnerSummaryResponse> listSummarized(String search, Long companyId, Pageable pageable) {
        log.info("listSummarized() - START - companyId={}, search={}", companyId, search);
        Page<PartnerSummaryResponse> page = listSummarized(search, null, null, null, companyId, pageable);
        log.info("listSummarized() - END - companyId={}, totalElements={}, totalPages={}", companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Transactional(readOnly = true)
    public Page<PartnerSummaryResponse> listSummarized(
            String search,
            Boolean isCustomer,
            Boolean isSupplier,
            Boolean isCarrier,
            Long companyId,
            Pageable pageable
    ) {
        log.info("listSummarized() - START - companyId={}, search={}, isCustomer={}, isSupplier={}, isCarrier={}", companyId, search, isCustomer, isSupplier, isCarrier);

        Page<PartnerSummaryResponse> page = partnerRepository.searchByCompanyIdAndTerm(companyId, search, isCustomer, isSupplier, isCarrier, pageable)
                .map(PartnerSummaryResponse::fromEntity);

        log.info("listSummarized() - END - companyId={}, totalElements={}, totalPages={}", companyId, page.getTotalElements(), page.getTotalPages());
        return page;
    }
}
