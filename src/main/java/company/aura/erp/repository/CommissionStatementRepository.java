package company.aura.erp.repository;

import company.aura.erp.dto.commission.CommissionStatementSummaryResponse;
import company.aura.erp.entity.CommissionStatementEntity;
import company.aura.erp.enums.CommissionStatementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionStatementRepository extends JpaRepository<CommissionStatementEntity, Long> {

    Optional<CommissionStatementEntity> findByIdAndCompanyId(Long id, Long companyId);

    Optional<CommissionStatementEntity> findByPayableIdAndCompanyId(Long payableId, Long companyId);

    @Query(
            value = """
                SELECT new company.aura.erp.dto.commission.CommissionStatementSummaryResponse(
                    s.id,
                    s.collaboratorId,
                    s.collaboratorName,
                    s.payableId,
                    s.periodStart,
                    s.periodEnd,
                    s.dueDate,
                    s.totalSalesAmount,
                    s.totalCommissionAmount,
                    s.status,
                    s.paidAt
                )
                FROM CommissionStatementEntity s
                WHERE s.companyId = :companyId
                  AND (:collaboratorId IS NULL OR s.collaboratorId = :collaboratorId)
                  AND (:hasStatuses = false OR s.status IN (:statuses))
                  AND (:startDate IS NULL OR s.periodStart >= :startDate)
                  AND (:endDate IS NULL OR s.periodEnd <= :endDate)
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(s.collaboratorName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       CAST(s.id AS string) LIKE CONCAT('%', :search, '%'))
            """,
            countQuery = """
                SELECT count(s)
                FROM CommissionStatementEntity s
                WHERE s.companyId = :companyId
                  AND (:collaboratorId IS NULL OR s.collaboratorId = :collaboratorId)
                  AND (:hasStatuses = false OR s.status IN (:statuses))
                  AND (:startDate IS NULL OR s.periodStart >= :startDate)
                  AND (:endDate IS NULL OR s.periodEnd <= :endDate)
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(s.collaboratorName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       CAST(s.id AS string) LIKE CONCAT('%', :search, '%'))
            """
    )
    Page<CommissionStatementSummaryResponse> searchStatements(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("collaboratorId") Long collaboratorId,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") List<CommissionStatementStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
