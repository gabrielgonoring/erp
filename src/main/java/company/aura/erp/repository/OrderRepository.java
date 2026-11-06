package company.aura.erp.repository;

import company.aura.erp.dto.order.OrderSummaryResponse;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByIdAndCompanyId(Long id, Long companyId);

    @Query(
            value = "SELECT new company.aura.erp.dto.order.OrderSummaryResponse(" +
                    "o.id, p.document, p.name, o.totalAmount, o.status, o.createdAt) " +
                    "FROM OrderEntity o JOIN PartnerEntity p ON o.partnerId = p.id " +
                    "WHERE o.companyId = :companyId AND " +
                    "(:search IS NULL OR :search = '' OR " +
                    "CAST(o.id AS string) LIKE CONCAT('%', :search, '%') OR " +
                    "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(CAST(o.status AS string)) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT count(o) " +
                    "FROM OrderEntity o JOIN PartnerEntity p ON o.partnerId = p.id " +
                    "WHERE o.companyId = :companyId AND " +
                    "(:search IS NULL OR :search = '' OR " +
                    "CAST(o.id AS string) LIKE CONCAT('%', :search, '%') OR " +
                    "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(CAST(o.status AS string)) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<OrderSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            Pageable pageable
    );
}
