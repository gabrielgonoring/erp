package company.aura.erp.repository;

import company.aura.erp.entity.ServiceOrderUpdateEntity;
import company.aura.erp.enums.ServiceOrderUpdateEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceOrderUpdateRepository extends JpaRepository<ServiceOrderUpdateEntity, Long> {

    Optional<ServiceOrderUpdateEntity> findByIdAndServiceOrderIdAndCompanyId(Long id, Long serviceOrderId, Long companyId);

    @Query(
            value = "SELECT u FROM ServiceOrderUpdateEntity u " +
                    "WHERE u.companyId = :companyId " +
                    "AND u.serviceOrderId = :serviceOrderId " +
                    "AND (:eventType IS NULL OR u.eventType = :eventType)",
            countQuery = "SELECT count(u) FROM ServiceOrderUpdateEntity u " +
                    "WHERE u.companyId = :companyId " +
                    "AND u.serviceOrderId = :serviceOrderId " +
                    "AND (:eventType IS NULL OR u.eventType = :eventType)"
    )
    Page<ServiceOrderUpdateEntity> searchByServiceOrder(
            @Param("companyId") Long companyId,
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("eventType") ServiceOrderUpdateEventType eventType,
            Pageable pageable
    );
}
