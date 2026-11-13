package company.aura.erp.service;

import company.aura.erp.dto.company.CompanyCreateRequest;
import company.aura.erp.dto.company.CompanyResponse;
import company.aura.erp.dto.company.CompanyUpdateRequest;
import company.aura.erp.entity.CompanyEntity;
import company.aura.erp.entity.UserEntity;
import company.aura.erp.enums.CompanyStatus;
import company.aura.erp.repository.CompanyRepository;
import company.aura.erp.repository.UserRepository;
import company.aura.erp.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;


    @Transactional
    public CompanyResponse save(CompanyCreateRequest request, UserPrincipal userPrincipal) {
        log.info("save() - START - userId={}, cnpj={}, corporateName={}", userPrincipal != null ? userPrincipal.getId() : null, request != null ? request.cnpj() : null, request != null ? request.corporateName() : null);

        if (userPrincipal.getCompanyId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário já está vinculada a uma empresa, caso deseje alterar as informações da empresa vá em configurações");
        }

        if (companyRepository.existsByCnpj(request.cnpj())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CNPJ já cadastrado: " + request.cnpj());
        }

        CompanyEntity company = fromCreateRequest(request);
        CompanyEntity saved = companyRepository.save(company);


        UserEntity user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setCompanyId(saved.getId());
        userRepository.save(user);

        CompanyResponse response = CompanyResponse.fromEntity(saved);
        log.info("save() - END - companyId={}, userId={}", response.id(), userPrincipal != null ? userPrincipal.getId() : null);
        return response;
    }

    private static CompanyEntity fromCreateRequest(CompanyCreateRequest request) {
        return CompanyEntity.builder()
                .corporateName(request.corporateName())
                .tradeName(request.tradeName())
                .cnpj(request.cnpj())
                .status(CompanyStatus.ACTIVE.name())
                .stateRegistration(request.stateRegistration())
                .municipalRegistration(request.municipalRegistration())
                .taxRegime(request.taxRegime() != null ? request.taxRegime() : 1)
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
    public CompanyResponse update(UserPrincipal userPrincipal, CompanyUpdateRequest request) {
        log.info("update() - START - companyId={}, userId={}", userPrincipal != null ? userPrincipal.getCompanyId() : null, userPrincipal != null ? userPrincipal.getId() : null);

        if (userPrincipal.getCompanyId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário não possui empresa vinculada entre em contato com o administrador");
        }

        CompanyEntity company = companyRepository.findById(userPrincipal.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada"));

        company.setCorporateName(request.corporateName());
        company.setTradeName(request.tradeName());
        company.setStateRegistration(request.stateRegistration());
        company.setMunicipalRegistration(request.municipalRegistration());
        company.setTaxRegime(request.taxRegime());
        company.setStreet(request.street());
        company.setNumber(request.number());
        company.setComplement(request.complement());
        company.setNeighborhood(request.neighborhood());
        company.setIbgeCityCode(request.ibgeCityCode());
        company.setCity(request.city());
        company.setState(request.state());
        company.setZipCode(request.zipCode());
        company.setCountryCode(request.countryCode());
        company.setCountryName(request.countryName());

        CompanyEntity updated = companyRepository.save(company);
        CompanyResponse response = CompanyResponse.fromEntity(updated);
        log.info("update() - END - companyId={}, userId={}", response.id(), userPrincipal != null ? userPrincipal.getId() : null);
        return response;
    }

    public CompanyResponse findById(UserPrincipal userPrincipal) {
        log.info("findById() - START - companyId={}, userId={}", userPrincipal != null ? userPrincipal.getCompanyId() : null, userPrincipal != null ? userPrincipal.getId() : null);

        if (userPrincipal.getCompanyId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário não possui empresa vinculada entre em contato com o administrador");
        }

        CompanyEntity company = companyRepository.findById(userPrincipal.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada"));

        CompanyResponse response = CompanyResponse.fromEntity(company);
        log.info("findById() - END - companyId={}, userId={}", response.id(), userPrincipal != null ? userPrincipal.getId() : null);
        return response;
    }
}
