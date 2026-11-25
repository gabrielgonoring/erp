package company.aura.erp.repository;

import company.aura.erp.dto.dashboard.SalesAggregationSummary;
import company.aura.erp.dto.dashboard.ServiceOrderStatusSummary;
import company.aura.erp.dto.serviceorder.ServiceOrderSummaryResponse;
import company.aura.erp.entity.ServiceOrderEntity;
import company.aura.erp.enums.ServiceOrderStatus;
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
public interface ServiceOrderRepository extends JpaRepository<ServiceOrderEntity, Long> {

    Optional<ServiceOrderEntity> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByOrderIdAndCompanyId(Long orderId, Long companyId);

    @Query(
            value = "SELECT new company.aura.erp.dto.serviceorder.ServiceOrderSummaryResponse(" +
                    "s.id, s.partnerId, p.document, p.name, s.technicianId, c.name, s.orderId, " +
                    "s.objectTitle, s.objectIdentifier, s.totalAmount, s.status, s.entryAt, s.expectedDeliveryAt, " +
                    "s.completedAt, s.createdAt) " +
                    "FROM ServiceOrderEntity s " +
                    "JOIN PartnerEntity p ON s.partnerId = p.id " +
                    "LEFT JOIN CollaboratorEntity c ON s.technicianId = c.id " +
                    "WHERE s.companyId = :companyId AND " +
                    "(:search IS NULL OR :search = '' OR " +
                    "CAST(s.id AS string) LIKE CONCAT('%', :search, '%') OR " +
                    "LOWER(s.objectTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "(s.objectIdentifier IS NOT NULL AND LOWER(s.objectIdentifier) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "(p.partnerIdentifier IS NOT NULL AND LOWER(p.partnerIdentifier) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "(p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "(c.name IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "LOWER(CAST(s.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                    "(:hasDateRange = false OR s.entryAt BETWEEN :startDateTime AND :endDateTime) AND " +
                    "(:hasStartDateOnly = false OR s.entryAt >= :startDateTime) AND " +
                    "(:hasEndDateOnly = false OR s.entryAt <= :endDateTime) AND " +
                    "(:hasStatuses = false OR s.status IN (:statuses)) AND " +
                    "(:partnerId IS NULL OR s.partnerId = :partnerId) AND " +
                    "(:technicianId IS NULL OR s.technicianId = :technicianId) AND " +
                    "(:objectIdentifier IS NULL OR :objectIdentifier = '' OR (s.objectIdentifier IS NOT NULL AND LOWER(s.objectIdentifier) LIKE LOWER(CONCAT('%', :objectIdentifier, '%'))))",
            countQuery = "SELECT count(s) " +
                    "FROM ServiceOrderEntity s " +
                    "JOIN PartnerEntity p ON s.partnerId = p.id " +
                    "LEFT JOIN CollaboratorEntity c ON s.technicianId = c.id " +
                    "WHERE s.companyId = :companyId AND " +
                    "(:search IS NULL OR :search = '' OR " +
                    "CAST(s.id AS string) LIKE CONCAT('%', :search, '%') OR " +
                    "LOWER(s.objectTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "(s.objectIdentifier IS NOT NULL AND LOWER(s.objectIdentifier) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "(p.partnerIdentifier IS NOT NULL AND LOWER(p.partnerIdentifier) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "(p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "(c.name IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "LOWER(CAST(s.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                    "(:hasDateRange = false OR s.entryAt BETWEEN :startDateTime AND :endDateTime) AND " +
                    "(:hasStartDateOnly = false OR s.entryAt >= :startDateTime) AND " +
                    "(:hasEndDateOnly = false OR s.entryAt <= :endDateTime) AND " +
                    "(:hasStatuses = false OR s.status IN (:statuses)) AND " +
                    "(:partnerId IS NULL OR s.partnerId = :partnerId) AND " +
                    "(:technicianId IS NULL OR s.technicianId = :technicianId) AND " +
                    "(:objectIdentifier IS NULL OR :objectIdentifier = '' OR (s.objectIdentifier IS NOT NULL AND LOWER(s.objectIdentifier) LIKE LOWER(CONCAT('%', :objectIdentifier, '%'))))"
    )
    Page<ServiceOrderSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("hasDateRange") boolean hasDateRange,
            @Param("hasStartDateOnly") boolean hasStartDateOnly,
            @Param("hasEndDateOnly") boolean hasEndDateOnly,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") List<ServiceOrderStatus> statuses,
            @Param("partnerId") Long partnerId,
            @Param("technicianId") Long technicianId,
            @Param("objectIdentifier") String objectIdentifier,
            Pageable pageable
    );

    @Query("""
        SELECT new company.aura.erp.dto.dashboard.ServiceOrderStatusSummary(
            so.status,
            COUNT(so),
            COALESCE(SUM(so.totalAmount), 0)
        )
        FROM ServiceOrderEntity so
        WHERE so.companyId = :companyId
          AND COALESCE(so.completedAt, so.createdAt) >= :startDate
          AND COALESCE(so.completedAt, so.createdAt) <= :endDate
        GROUP BY so.status
    """)
    List<ServiceOrderStatusSummary> summarizeByStatusesAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT new company.aura.erp.dto.dashboard.SalesAggregationSummary(
            COALESCE(SUM(so.totalAmount), 0),
            COUNT(so)
        )
        FROM ServiceOrderEntity so
        WHERE so.companyId = :companyId
          AND so.status <> :canceledStatus
          AND so.createdAt >= :startDate
          AND so.createdAt <= :endDate
    """)
    SalesAggregationSummary getServiceOrdersSalesSummary(
            @Param("companyId") Long companyId,
            @Param("canceledStatus") ServiceOrderStatus canceledStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
