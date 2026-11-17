package company.aura.erp.repository;

import company.aura.erp.dto.dashboard.InstallmentPaymentSummary;
import company.aura.erp.dto.dashboard.PendingInstallmentSummary;
import company.aura.erp.dto.order.OrderInstallmentSummaryResponse;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.OrderStatus;
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
public interface OrderInstallmentRepository extends JpaRepository<OrderInstallmentEntity, Long> {

    List<OrderInstallmentEntity> findByOrderId(Long orderId);

    Optional<OrderInstallmentEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT COALESCE(MAX(oi.installmentNumber), 0) FROM OrderInstallmentEntity oi WHERE oi.orderId = :orderId")
    Integer findMaxInstallmentNumberByOrderId(@Param("orderId") Long orderId);

    @Query("""
        SELECT new company.aura.erp.dto.dashboard.InstallmentPaymentSummary(
            COALESCE(SUM(oi.amount - oi.pendingAmount), 0),
            COALESCE(SUM(oi.pendingAmount), 0)
        )
        FROM OrderInstallmentEntity oi
        JOIN OrderEntity o ON oi.orderId = o.id
        WHERE o.companyId = :companyId
          AND o.status IN (:orderStatuses)
          AND oi.status NOT IN (:notInInstallmentStatus)
          AND COALESCE(o.completedAt, o.createdAt) >= :startDate
          AND COALESCE(o.completedAt, o.createdAt) <= :endDate
    """)
    InstallmentPaymentSummary getPaymentsSummaryByOrderDate(
            @Param("companyId") Long companyId,
            @Param("orderStatuses") List<OrderStatus> orderStatuses,
            @Param("notInInstallmentStatus") List<InstallmentStatus> notInInstallmentStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT new company.aura.erp.dto.dashboard.PendingInstallmentSummary(
            COUNT(oi),
            COALESCE(SUM(oi.pendingAmount), 0)
        )
        FROM OrderInstallmentEntity oi
        WHERE oi.companyId = :companyId
          AND oi.status != company.aura.erp.enums.InstallmentStatus.CANCELED
          AND oi.pendingAmount > 0
          AND oi.dueDate >= :startDate
          AND oi.dueDate <= :endDate
    """)
    PendingInstallmentSummary getPendingInstallmentsSummaryByDueDate(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(
            value = """
                SELECT new company.aura.erp.dto.order.OrderInstallmentSummaryResponse(
                    oi.id,
                    oi.orderId,
                    p.document,
                    p.name,
                    oi.amount,
                    oi.pendingAmount,
                    oi.status,
                    oi.paymentMethod,
                    oi.dueDate
                )
                FROM OrderInstallmentEntity oi
                LEFT JOIN PartnerEntity p ON oi.partnerId = p.id
                WHERE oi.companyId = :companyId
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       (p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))))
                  AND (:hasDateRange = false OR oi.dueDate BETWEEN :startDate AND :endDate)
                  AND (:hasStartDateOnly = false OR oi.dueDate >= :startDate)
                  AND (:hasEndDateOnly = false OR oi.dueDate <= :endDate)
                  AND (:hasStatuses = false OR oi.status IN (:statuses))
            """,
            countQuery = """
                SELECT count(oi)
                FROM OrderInstallmentEntity oi
                LEFT JOIN PartnerEntity p ON oi.partnerId = p.id
                WHERE oi.companyId = :companyId
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       (p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))))
                  AND (:hasDateRange = false OR oi.dueDate BETWEEN :startDate AND :endDate)
                  AND (:hasStartDateOnly = false OR oi.dueDate >= :startDate)
                  AND (:hasEndDateOnly = false OR oi.dueDate <= :endDate)
                  AND (:hasStatuses = false OR oi.status IN (:statuses))
            """
    )
    Page<OrderInstallmentSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("hasDateRange") boolean hasDateRange,
            @Param("hasStartDateOnly") boolean hasStartDateOnly,
            @Param("hasEndDateOnly") boolean hasEndDateOnly,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") List<InstallmentStatus> statuses,
            Pageable pageable
    );
}
