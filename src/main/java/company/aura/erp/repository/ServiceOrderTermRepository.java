package company.aura.erp.repository;

import company.aura.erp.dto.serviceorder.ServiceOrderTermSummaryResponse;
import company.aura.erp.entity.ServiceOrderTermEntity;
import company.aura.erp.enums.ServiceOrderTermStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderTermRepository extends JpaRepository<ServiceOrderTermEntity, Long> {

    Optional<ServiceOrderTermEntity> findByIdAndCompanyIdAndStatusNot(Long id, Long companyId, ServiceOrderTermStatus status);

    List<ServiceOrderTermEntity> findByCompanyIdAndIsDefaultTrue(Long companyId);

    @Query(
            value = "SELECT new company.aura.erp.dto.serviceorder.ServiceOrderTermSummaryResponse(t.id, t.title, t.status, t.isDefault) " +
                    "FROM ServiceOrderTermEntity t " +
                    "WHERE t.companyId = :companyId " +
                    "AND t.status <> :deletedStatus " +
                    "AND (:search IS NULL OR :search = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR t.status = :status)",
            countQuery = "SELECT count(t) " +
                    "FROM ServiceOrderTermEntity t " +
                    "WHERE t.companyId = :companyId " +
                    "AND t.status <> :deletedStatus " +
                    "AND (:search IS NULL OR :search = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR t.status = :status)"
    )
    Page<ServiceOrderTermSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("deletedStatus") ServiceOrderTermStatus deletedStatus,
            @Param("search") String search,
            @Param("status") ServiceOrderTermStatus status,
            Pageable pageable
    );
}
