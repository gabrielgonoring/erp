package company.aura.erp.repository;

import company.aura.erp.dto.order.OrderSummaryResponse;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByIdAndCompanyId(Long id, Long companyId);

    @Query(
            value = "SELECT new company.aura.erp.dto.order.OrderSummaryResponse(" +
                    "o.id, p.document, p.name, o.totalAmount, o.status, o.createdAt) " +
                    "FROM OrderEntity o JOIN PartnerEntity p ON o.partnerId = p.id " +
                    "WHERE o.companyId = :companyId AND " +
                    "(:search IS NULL OR :search = '' OR " +
                    "CAST(o.id AS string) LIKE CONCAT('%', :search, '%') OR " +
                    "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "(p.partnerIdentifier IS NOT NULL AND LOWER(p.partnerIdentifier) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "(p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "LOWER(CAST(o.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                    "(:hasDateRange = false OR o.createdAt BETWEEN :startDateTime AND :endDateTime) AND " +
                    "(:hasStartDateOnly = false OR o.createdAt >= :startDateTime) AND " +
                    "(:hasEndDateOnly = false OR o.createdAt <= :endDateTime) AND " +
                    "(:hasStatuses = false OR o.status IN (:statuses))",
            countQuery = "SELECT count(o) " +
                    "FROM OrderEntity o JOIN PartnerEntity p ON o.partnerId = p.id " +
                    "WHERE o.companyId = :companyId AND " +
                    "(:search IS NULL OR :search = '' OR " +
                    "CAST(o.id AS string) LIKE CONCAT('%', :search, '%') OR " +
                    "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "(p.partnerIdentifier IS NOT NULL AND LOWER(p.partnerIdentifier) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "(p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
                    "LOWER(CAST(o.status AS string)) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                    "(:hasDateRange = false OR o.createdAt BETWEEN :startDateTime AND :endDateTime) AND " +
                    "(:hasStartDateOnly = false OR o.createdAt >= :startDateTime) AND " +
                    "(:hasEndDateOnly = false OR o.createdAt <= :endDateTime) AND " +
                    "(:hasStatuses = false OR o.status IN (:statuses))"
    )
    Page<OrderSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("hasDateRange") boolean hasDateRange,
            @Param("hasStartDateOnly") boolean hasStartDateOnly,
            @Param("hasEndDateOnly") boolean hasEndDateOnly,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable
    );

    default Page<OrderSummaryResponse> searchSummarized(Long companyId, String search, Pageable pageable) {
        return searchSummarized(companyId, search, false, false, false, null, null, false, List.of(OrderStatus.OPEN), pageable);
    }

    @Query("SELECT SUM(o.totalAmount) FROM OrderEntity o " +
            "WHERE o.companyId = :companyId AND o.status = :status " +
            "AND COALESCE(o.completedAt, o.createdAt) >= :startDate AND COALESCE(o.completedAt, o.createdAt) <= :endDate")
    BigDecimal sumAmountByStatusAndDateRange(
            @Param("companyId") Long companyId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(o) FROM OrderEntity o " +
            "WHERE o.companyId = :companyId AND o.status = :status " +
            "AND COALESCE(o.completedAt, o.createdAt) >= :startDate AND COALESCE(o.completedAt, o.createdAt) <= :endDate")
    long countByStatusAndDateRange(
            @Param("companyId") Long companyId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(o) FROM OrderEntity o " +
            "WHERE o.companyId = :companyId AND o.status IN (:statuses)")
    long countByStatuses(
            @Param("companyId") Long companyId,
            @Param("statuses") List<OrderStatus> statuses
    );

    @Query("SELECT SUM(o.totalAmount) FROM OrderEntity o " +
            "WHERE o.companyId = :companyId AND o.status IN (:statuses)")
    BigDecimal sumAmountByStatuses(
            @Param("companyId") Long companyId,
            @Param("statuses") List<OrderStatus> statuses
    );
}
