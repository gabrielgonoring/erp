package company.aura.erp.repository;

import company.aura.erp.dto.dashboard.PendingReceivableSummary;
import company.aura.erp.dto.dashboard.ReceivablePaymentSummary;
import company.aura.erp.dto.job.ReceivableDueNotice;
import company.aura.erp.dto.order.ReceivableSummaryResponse;
import company.aura.erp.entity.ReceivableEntity;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.enums.ReceivableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableRepository extends JpaRepository<ReceivableEntity, Long> {

    List<ReceivableEntity> findByOrderId(Long orderId);

    List<ReceivableEntity> findByServiceOrderId(Long serviceOrderId);

    Optional<ReceivableEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT COALESCE(MAX(r.installmentNumber), 0) FROM ReceivableEntity r WHERE r.orderId = :orderId")
    Integer findMaxInstallmentNumberByOrderId(@Param("orderId") Long orderId);

    @Query("""
        SELECT new company.aura.erp.dto.dashboard.ReceivablePaymentSummary(
            COALESCE(SUM(r.amount - r.pendingAmount), 0),
            COALESCE(SUM(r.pendingAmount), 0)
        )
        FROM ReceivableEntity r
        JOIN OrderEntity o ON r.orderId = o.id
        WHERE o.companyId = :companyId
          AND o.status IN (:orderStatuses)
          AND r.status NOT IN (:notInReceivableStatus)
          AND COALESCE(o.completedAt, o.createdAt) >= :startDate
          AND COALESCE(o.completedAt, o.createdAt) <= :endDate
    """)
    ReceivablePaymentSummary getPaymentsSummaryByOrderDate(
            @Param("companyId") Long companyId,
            @Param("orderStatuses") List<OrderStatus> orderStatuses,
            @Param("notInReceivableStatus") List<ReceivableStatus> notInReceivableStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT new company.aura.erp.dto.dashboard.PendingReceivableSummary(
            COUNT(r),
            COALESCE(SUM(r.pendingAmount), 0)
        )
        FROM ReceivableEntity r
        WHERE r.companyId = :companyId
          AND r.status != company.aura.erp.enums.ReceivableStatus.CANCELED
          AND r.pendingAmount > 0
          AND r.dueDate >= :startDate
          AND r.dueDate <= :endDate
    """)
    PendingReceivableSummary getPendingReceivablesSummaryByDueDate(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(
            value = """
                SELECT new company.aura.erp.dto.order.ReceivableSummaryResponse(
                    r.id,
                    r.orderId,
                    r.serviceOrderId,
                    p.document,
                    p.name,
                    r.amount,
                    r.pendingAmount,
                    r.status,
                    r.paymentMethod,
                    r.dueDate
                )
                FROM ReceivableEntity r
                LEFT JOIN PartnerEntity p ON r.partnerId = p.id
                WHERE r.companyId = :companyId
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       (p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))))
                  AND (:hasDateRange = false OR r.dueDate BETWEEN :startDate AND :endDate)
                  AND (:hasStartDateOnly = false OR r.dueDate >= :startDate)
                  AND (:hasEndDateOnly = false OR r.dueDate <= :endDate)
                  AND (:hasStatuses = false OR r.status IN (:statuses))
            """,
            countQuery = """
                SELECT count(r)
                FROM ReceivableEntity r
                LEFT JOIN PartnerEntity p ON r.partnerId = p.id
                WHERE r.companyId = :companyId
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       (p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))))
                  AND (:hasDateRange = false OR r.dueDate BETWEEN :startDate AND :endDate)
                  AND (:hasStartDateOnly = false OR r.dueDate >= :startDate)
                  AND (:hasEndDateOnly = false OR r.dueDate <= :endDate)
                  AND (:hasStatuses = false OR r.status IN (:statuses))
            """
    )
    Page<ReceivableSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("hasDateRange") boolean hasDateRange,
            @Param("hasStartDateOnly") boolean hasStartDateOnly,
            @Param("hasEndDateOnly") boolean hasEndDateOnly,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") List<ReceivableStatus> statuses,
            Pageable pageable
    );

    @Query(
            value = """
                SELECT new company.aura.erp.dto.job.ReceivableDueNotice(
                    r.id,
                    r.orderId,
                    r.serviceOrderId,
                    r.installmentNumber,
                    r.amount,
                    r.pendingAmount,
                    r.dueDate,
                    r.paymentMethod,
                    r.paymentFileUrl,
                    r.paymentUrl,
                    r.bankSlipCode,
                    r.companyId,
                    p.name,
                    p.email,
                    p.billingEmail
                )
                FROM ReceivableEntity r
                LEFT JOIN PartnerEntity p ON r.partnerId = p.id
                WHERE r.dueDate = :dueDate
                  AND r.status NOT IN (:excludedStatuses)
                  AND r.pendingAmount > 0
            """,
            countQuery = """
                SELECT count(r)
                FROM ReceivableEntity r
                LEFT JOIN PartnerEntity p ON r.partnerId = p.id
                WHERE r.dueDate = :dueDate
                  AND r.status NOT IN (:excludedStatuses)
                  AND r.pendingAmount > 0
            """
    )
    Page<ReceivableDueNotice> findReceivablesDueNotice(
            @Param("dueDate") LocalDate dueDate,
            @Param("excludedStatuses") List<ReceivableStatus> excludedStatuses,
            Pageable pageable
    );
}
