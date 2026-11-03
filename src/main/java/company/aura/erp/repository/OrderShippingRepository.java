package company.aura.erp.repository;

import company.aura.erp.entity.OrderShippingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderShippingRepository extends JpaRepository<OrderShippingEntity, Long> {

    Optional<OrderShippingEntity> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
