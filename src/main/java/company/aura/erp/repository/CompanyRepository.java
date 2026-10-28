package company.aura.erp.repository;

import company.aura.erp.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

    Optional<CompanyEntity> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
