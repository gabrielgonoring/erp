package company.aura.erp.repository;

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
//
//    @Query("SELECT new company.aura.erp.dto.dashboard.OrderAggregation(COUNT(o), SUM(p.amountPaid)) " +
//            "FROM OrderEntity o " +
//            "INNER JOIN OrderInstallmentEntity oi ON oi.orderId = o.id " +
//            "INNER JOIN PaymentTransactionEntity p ON p.installmentId = oi.id " +
//            "WHERE o.companyId = :companyId AND oi.status in :status " +
//            "AND COALESCE(o.completedAt, o.createdAt) >= :startDate AND COALESCE(o.completedAt, o.createdAt) <= :endDate")
//    OrderAggregation aggregateByStatusAndDateRange(
//            @Param("companyId") Long companyId,
//            @Param("status") OrderStatus status,
//            @Param("startDate") LocalDateTime startDate,
//            @Param("endDate") LocalDateTime endDate
//    );
//
//    @Query("SELECT new company.aura.erp.dto.dashboard.OrderAggregation(COUNT(o), SUM(o.totalAmount)) " +
//            "FROM OrderEntity o " +
//            "WHERE o.companyId = :companyId AND o.status IN (:statuses)")
//    OrderAggregation aggregateByStatuses(
//            @Param("companyId") Long companyId,
//            @Param("statuses") List<OrderStatus> statuses
//    );
//
//    @Query("SELECT SUM(o.totalAmount) FROM OrderEntity o " +
//            "WHERE o.companyId = :companyId AND o.status = :status " +
//            "AND COALESCE(o.completedAt, o.createdAt) >= :startDate AND COALESCE(o.completedAt, o.createdAt) <= :endDate")
//    BigDecimal sumAmountByStatusAndDateRange(
//            @Param("companyId") Long companyId,
//            @Param("status") OrderStatus status,
//            @Param("startDate") LocalDateTime startDate,
//            @Param("endDate") LocalDateTime endDate
//    );

}
