package company.aura.erp.repository;

import company.aura.erp.entity.CommissionEntryEntity;
import company.aura.erp.enums.CommissionEntryStatus;
import company.aura.erp.enums.CommissionSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionEntryRepository extends JpaRepository<CommissionEntryEntity, Long> {

    Optional<CommissionEntryEntity> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndSourceTypeAndSourceItemId(Long companyId, CommissionSourceType sourceType, Long sourceItemId);

    List<CommissionEntryEntity> findByCompanyIdAndSourceTypeAndSourceItemIdInAndStatusNot(
            Long companyId,
            CommissionSourceType sourceType,
            List<Long> sourceItemIds,
            CommissionEntryStatus status
    );

    List<CommissionEntryEntity> findByStatementIdAndCompanyIdOrderByOccurredAtDescIdDesc(Long statementId, Long companyId);

    List<CommissionEntryEntity> findByCompanyIdAndCollaboratorIdAndStatusAndStatementIdIsNullAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(
            Long companyId,
            Long collaboratorId,
            CommissionEntryStatus status,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    List<CommissionEntryEntity> findAllByIdInAndCompanyId(List<Long> ids, Long companyId);

    @Query(
            value = """
                SELECT e
                FROM CommissionEntryEntity e
                WHERE e.companyId = :companyId
                  AND (:collaboratorId IS NULL OR e.collaboratorId = :collaboratorId)
                  AND (:statementId IS NULL OR e.statementId = :statementId)
                  AND (:hasStatuses = false OR e.status IN (:statuses))
                  AND (:sourceType IS NULL OR e.sourceType = :sourceType)
                  AND (:hasDateRange = false OR e.occurredAt BETWEEN :startDateTime AND :endDateTime)
                  AND (:hasStartDateOnly = false OR e.occurredAt >= :startDateTime)
                  AND (:hasEndDateOnly = false OR e.occurredAt <= :endDateTime)
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(e.collaboratorName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       LOWER(e.itemDescription) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
                SELECT count(e)
                FROM CommissionEntryEntity e
                WHERE e.companyId = :companyId
                  AND (:collaboratorId IS NULL OR e.collaboratorId = :collaboratorId)
                  AND (:statementId IS NULL OR e.statementId = :statementId)
                  AND (:hasStatuses = false OR e.status IN (:statuses))
                  AND (:sourceType IS NULL OR e.sourceType = :sourceType)
                  AND (:hasDateRange = false OR e.occurredAt BETWEEN :startDateTime AND :endDateTime)
                  AND (:hasStartDateOnly = false OR e.occurredAt >= :startDateTime)
                  AND (:hasEndDateOnly = false OR e.occurredAt <= :endDateTime)
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(e.collaboratorName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       LOWER(e.itemDescription) LIKE LOWER(CONCAT('%', :search, '%')))
            """
    )
    Page<CommissionEntryEntity> searchEntries(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("collaboratorId") Long collaboratorId,
            @Param("statementId") Long statementId,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") List<CommissionEntryStatus> statuses,
            @Param("sourceType") CommissionSourceType sourceType,
            @Param("hasDateRange") boolean hasDateRange,
            @Param("hasStartDateOnly") boolean hasStartDateOnly,
            @Param("hasEndDateOnly") boolean hasEndDateOnly,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable
    );
}
