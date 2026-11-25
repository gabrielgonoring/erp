package company.aura.erp.repository;

import company.aura.erp.dto.dashboard.RealizedRevenueSummary;
import company.aura.erp.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {

    List<PaymentTransactionEntity> findByReceivableId(Long receivableId);

    Optional<PaymentTransactionEntity> findByIdAndCompanyId(Long id, Long companyId);

    Optional<PaymentTransactionEntity> findByIdAndReceivableIdAndCompanyId(Long id, Long receivableId, Long companyId);

    List<PaymentTransactionEntity> findByCompanyId(Long companyId);

    @Query("""
        SELECT new company.aura.erp.dto.dashboard.RealizedRevenueSummary(
            COALESCE(SUM(
                CASE
                    WHEN pt.paidAt >= :startDate AND pt.paidAt <= :endDate AND pt.transactionType = company.aura.erp.enums.PaymentTransactionType.PAYMENT THEN pt.amountPaid
                    WHEN pt.paidAt >= :startDate AND pt.paidAt <= :endDate AND pt.transactionType = company.aura.erp.enums.PaymentTransactionType.REFUND THEN -pt.amountPaid
                END
            ), 0),
            COUNT(
                CASE WHEN pt.paidAt >= :startDate AND pt.paidAt <= :endDate AND pt.transactionType = company.aura.erp.enums.PaymentTransactionType.PAYMENT THEN 1 END
            ),
            COALESCE(SUM(
                CASE
                    WHEN pt.paidAt >= :prevStartDate AND pt.paidAt < :startDate AND pt.transactionType = company.aura.erp.enums.PaymentTransactionType.PAYMENT THEN pt.amountPaid
                    WHEN pt.paidAt >= :prevStartDate AND pt.paidAt < :startDate AND pt.transactionType = company.aura.erp.enums.PaymentTransactionType.REFUND THEN -pt.amountPaid
                END
            ), 0)
        )
        FROM PaymentTransactionEntity pt
        WHERE pt.companyId = :companyId
          AND pt.paidAt >= :prevStartDate
          AND pt.paidAt <= :endDate
    """)
    RealizedRevenueSummary getRealizedRevenueSummary(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("prevStartDate") LocalDateTime prevStartDate
    );

}
