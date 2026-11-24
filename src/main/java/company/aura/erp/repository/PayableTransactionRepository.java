package company.aura.erp.repository;

import company.aura.erp.entity.PayableTransactionEntity;
import company.aura.erp.enums.PayableTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayableTransactionRepository extends JpaRepository<PayableTransactionEntity, Long> {

    List<PayableTransactionEntity> findByPayableId(Long payableId);

    Optional<PayableTransactionEntity> findByIdAndPayableIdAndCompanyId(Long id, Long payableId, Long companyId);

    Optional<PayableTransactionEntity> findTopByPayableIdAndCompanyIdAndTransactionTypeOrderByPaidAtDescIdDesc(
            Long payableId,
            Long companyId,
            PayableTransactionType transactionType
    );

    Optional<PayableTransactionEntity> findTopByPayableIdAndCompanyIdOrderByPaidAtDescIdDesc(
            Long payableId,
            Long companyId
    );
}
