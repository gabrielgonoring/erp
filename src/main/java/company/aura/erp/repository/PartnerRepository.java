package company.aura.erp.repository;

import company.aura.erp.entity.PartnerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {

    List<PartnerEntity> findByCompanyId(Long companyId);

    Optional<PartnerEntity> findByIdAndCompanyId(Long id, Long companyId);

    Optional<PartnerEntity> findByIdAndCompanyIdAndDeletedAtIsNull(Long id, Long companyId);

    Optional<PartnerEntity> findByIdAndCompanyIdAndStatusNot(Long id, Long companyId, String status);

    List<PartnerEntity> findByCompanyIdAndIsCustomerTrue(Long companyId);

    List<PartnerEntity> findByCompanyIdAndIsSupplierTrue(Long companyId);

    @Query("SELECT p FROM PartnerEntity p WHERE p.companyId = :companyId AND (p.status IS NULL OR p.status <> 'DELETED') AND p.deletedAt IS NULL AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PartnerEntity> searchByCompanyIdAndTerm(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            Pageable pageable
    );
}
