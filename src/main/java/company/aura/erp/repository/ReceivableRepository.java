package company.aura.erp.repository;

import company.aura.erp.dto.dashboard.ReceivablesKpiSummary;
import company.aura.erp.dto.job.ReceivableDueNotice;
import company.aura.erp.dto.order.ReceivableSummaryResponse;
import company.aura.erp.entity.ReceivableEntity;
import company.aura.erp.enums.PaymentMethod;
import company.aura.erp.enums.ReceivableStatus;
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
public interface ReceivableRepository extends JpaRepository<ReceivableEntity, Long> {

    List<ReceivableEntity> findByOrderId(Long orderId);

    List<ReceivableEntity> findByServiceOrderId(Long serviceOrderId);

    boolean existsByOrderIdAndCompanyIdAndStatusNot(Long orderId, Long companyId, ReceivableStatus status);

    Optional<ReceivableEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT COALESCE(MAX(r.installmentNumber), 0) FROM ReceivableEntity r WHERE r.orderId = :orderId")
    Integer findMaxInstallmentNumberByOrderId(@Param("orderId") Long orderId);

    @Query("""
        SELECT new company.aura.erp.dto.dashboard.ReceivablesKpiSummary(
            COALESCE(SUM(CASE WHEN r.dueDate >= :today THEN r.pendingAmount END), 0),
            COUNT(CASE WHEN r.dueDate >= :today THEN 1 END),
            COALESCE(SUM(CASE WHEN r.dueDate >= :today AND r.dueDate <= :weekEndDate THEN r.pendingAmount END), 0),
            COUNT(CASE WHEN r.dueDate >= :today AND r.dueDate <= :weekEndDate THEN 1 END),
            COALESCE(SUM(CASE WHEN r.dueDate < :today THEN r.pendingAmount END), 0),
            COUNT(CASE WHEN r.dueDate < :today THEN 1 END),
            COUNT(DISTINCT CASE WHEN r.dueDate < :today THEN r.partnerId END)
        )
        FROM ReceivableEntity r
        WHERE r.companyId = :companyId
          AND r.pendingAmount > 0
          AND r.status IN (:openStatuses)
    """)
    ReceivablesKpiSummary getReceivablesKpiSummary(
            @Param("companyId") Long companyId,
            @Param("today") LocalDate today,
            @Param("weekEndDate") LocalDate weekEndDate,
            @Param("openStatuses") List<ReceivableStatus> openStatuses
    );

    @Query(
            value = """
                SELECT new company.aura.erp.dto.order.ReceivableSummaryResponse(
                    r.id,
                    r.orderId,
                    r.serviceOrderId,
                    r.originType,
                    p.id,
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
                  AND (:hasPaymentMethods = false OR r.paymentMethod IN (:paymentMethods))
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
                  AND (:hasPaymentMethods = false OR r.paymentMethod IN (:paymentMethods))
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
            @Param("hasPaymentMethods") boolean hasPaymentMethods,
            @Param("paymentMethods") List<PaymentMethod> paymentMethods,
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
