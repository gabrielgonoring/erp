package company.aura.erp.repository;

import company.aura.erp.dto.company.CompanyTermSummaryResponse;
import company.aura.erp.entity.CompanyTermEntity;
import company.aura.erp.enums.CompanyTermStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyTermRepository extends JpaRepository<CompanyTermEntity, Long> {

    Optional<CompanyTermEntity> findByIdAndCompanyIdAndStatusNot(Long id, Long companyId, CompanyTermStatus status);

    List<CompanyTermEntity> findByCompanyIdAndIsDefaultTrue(Long companyId);

    @Query(
            value = "SELECT new company.aura.erp.dto.company.CompanyTermSummaryResponse(t.id, t.title, t.status, t.isDefault) " +
                    "FROM CompanyTermEntity t " +
                    "WHERE t.companyId = :companyId " +
                    "AND t.status <> :deletedStatus " +
                    "AND (:search IS NULL OR :search = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR t.status = :status)",
            countQuery = "SELECT count(t) " +
                    "FROM CompanyTermEntity t " +
                    "WHERE t.companyId = :companyId " +
                    "AND t.status <> :deletedStatus " +
                    "AND (:search IS NULL OR :search = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR t.status = :status)"
    )
    Page<CompanyTermSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("deletedStatus") CompanyTermStatus deletedStatus,
            @Param("search") String search,
            @Param("status") CompanyTermStatus status,
            Pageable pageable
    );
}
