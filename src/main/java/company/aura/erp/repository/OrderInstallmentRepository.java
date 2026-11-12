package company.aura.erp.repository;

import company.aura.erp.dto.dashboard.InstallmentPaymentSummary;
import company.aura.erp.dto.dashboard.OrderAggregation;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.enums.InstallmentStatus;
import company.aura.erp.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
    InstallmentPaymentSummary getPaymentsSummary(
            @Param("companyId") Long companyId,
            @Param("orderStatuses") List<OrderStatus> orderStatuses,
            @Param("notInInstallmentStatus") List<InstallmentStatus> notInInstallmentStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
