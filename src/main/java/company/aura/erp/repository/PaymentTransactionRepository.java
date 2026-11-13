package company.aura.erp.repository;

import company.aura.erp.dto.dashboard.PaymentTransactionSummary;
import company.aura.erp.entity.PaymentTransactionEntity;
import company.aura.erp.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {

    List<PaymentTransactionEntity> findByInstallmentId(Long installmentId);

    Optional<PaymentTransactionEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<PaymentTransactionEntity> findByCompanyId(Long companyId);


    @Query("""
        SELECT new company.aura.erp.dto.dashboard.PaymentTransactionSummary(
            COUNT(pt),
            COALESCE(SUM(pt.amountPaid), 0)
        )
        FROM PaymentTransactionEntity pt
        JOIN OrderInstallmentEntity oi ON pt.installmentId = oi.id
        WHERE pt.companyId = :companyId
          AND oi.status in :status
          AND pt.paidAt >= :startDate
          AND pt.paidAt <= :endDate
    """)
    PaymentTransactionSummary getPaymentsSummaryByStatusInAndDateRange(
            @Param("companyId") Long companyId,
            @Param("status") List<InstallmentStatus> status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}
