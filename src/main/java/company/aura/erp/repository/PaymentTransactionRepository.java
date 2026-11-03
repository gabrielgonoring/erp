package company.aura.erp.repository;

import company.aura.erp.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {

    List<PaymentTransactionEntity> findByInstallmentId(Long installmentId);

    Optional<PaymentTransactionEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<PaymentTransactionEntity> findByCompanyId(Long companyId);
}
