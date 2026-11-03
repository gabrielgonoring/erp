package company.aura.erp.repository;

import company.aura.erp.entity.OrderEntity;
import company.aura.erp.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<OrderEntity> findByCompanyId(Long companyId);

    Page<OrderEntity> findByCompanyId(Long companyId, Pageable pageable);

    Page<OrderEntity> findByCompanyIdAndStatus(Long companyId, OrderStatus status, Pageable pageable);

    List<OrderEntity> findByCompanyIdAndPartnerId(Long companyId, Long partnerId);
}
