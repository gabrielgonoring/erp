package company.aura.erp.repository;

import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.enums.InstallmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderInstallmentRepository extends JpaRepository<OrderInstallmentEntity, Long> {

    List<OrderInstallmentEntity> findByOrderId(Long orderId);

    Optional<OrderInstallmentEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT COALESCE(MAX(oi.installmentNumber), 0) FROM OrderInstallmentEntity oi WHERE oi.orderId = :orderId")
    Integer findMaxInstallmentNumberByOrderId(Long orderId);
}
