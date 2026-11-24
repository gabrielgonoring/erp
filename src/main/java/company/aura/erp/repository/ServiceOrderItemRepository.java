package company.aura.erp.repository;

import company.aura.erp.entity.ServiceOrderItemEntity;
import company.aura.erp.enums.ServiceOrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderItemRepository extends JpaRepository<ServiceOrderItemEntity, Long> {

    List<ServiceOrderItemEntity> findByServiceOrderIdAndStatusNotIn(Long serviceOrderId, List<ServiceOrderItemStatus> statuses);

}
