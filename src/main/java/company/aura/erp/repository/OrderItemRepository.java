package company.aura.erp.repository;

import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    List<OrderItemEntity> findByProfessionalIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long professionalId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT oi FROM OrderItemEntity oi JOIN OrderEntity o ON oi.orderId = o.id " +
            "WHERE oi.professionalId = :collaboratorId " +
            "AND (:companyId IS NULL OR o.companyId = :companyId) " +
            "AND (:statuses IS NULL OR o.status in :statuses) " +
            "AND oi.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY oi.createdAt DESC")
    List<OrderItemEntity> findByCollaboratorAndCompanyAndCreatedAtBetween(
            @Param("collaboratorId") Long collaboratorId,
            @Param("companyId") Long companyId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
