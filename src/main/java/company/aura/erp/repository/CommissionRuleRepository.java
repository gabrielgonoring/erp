package company.aura.erp.repository;

import company.aura.erp.dto.commission.CommissionRuleSummaryResponse;
import company.aura.erp.entity.CommissionRuleEntity;
import company.aura.erp.enums.CommissionRuleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionRuleRepository extends JpaRepository<CommissionRuleEntity, Long> {

    Optional<CommissionRuleEntity> findByIdAndCompanyIdAndStatusNot(Long id, Long companyId, CommissionRuleStatus status);

    List<CommissionRuleEntity> findByCompanyIdAndStatus(Long companyId, CommissionRuleStatus status);

    @Query(
            value = """
                SELECT new company.aura.erp.dto.commission.CommissionRuleSummaryResponse(
                    r.id,
                    r.collaboratorId,
                    col.name,
                    r.categoryId,
                    cat.name,
                    r.catalogItemId,
                    ci.description,
                    r.commissionPercentage,
                    r.validFrom,
                    r.validUntil,
                    r.status
                )
                FROM CommissionRuleEntity r
                LEFT JOIN CollaboratorEntity col ON r.collaboratorId = col.id
                LEFT JOIN CatalogCategoryEntity cat ON r.categoryId = cat.id
                LEFT JOIN CatalogItemEntity ci ON r.catalogItemId = ci.id
                WHERE r.companyId = :companyId
                  AND (:collaboratorId IS NULL OR r.collaboratorId = :collaboratorId)
                  AND (:categoryId IS NULL OR r.categoryId = :categoryId)
                  AND (:catalogItemId IS NULL OR r.catalogItemId = :catalogItemId)
                  AND (:status IS NULL OR r.status = :status)
                  AND (:search IS NULL OR :search = '' OR
                       (col.name IS NOT NULL AND LOWER(col.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (cat.name IS NOT NULL AND LOWER(cat.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (ci.description IS NOT NULL AND LOWER(ci.description) LIKE LOWER(CONCAT('%', :search, '%'))))
            """,
            countQuery = """
                SELECT count(r)
                FROM CommissionRuleEntity r
                LEFT JOIN CollaboratorEntity col ON r.collaboratorId = col.id
                LEFT JOIN CatalogCategoryEntity cat ON r.categoryId = cat.id
                LEFT JOIN CatalogItemEntity ci ON r.catalogItemId = ci.id
                WHERE r.companyId = :companyId
                  AND (:collaboratorId IS NULL OR r.collaboratorId = :collaboratorId)
                  AND (:categoryId IS NULL OR r.categoryId = :categoryId)
                  AND (:catalogItemId IS NULL OR r.catalogItemId = :catalogItemId)
                  AND (:status IS NULL OR r.status = :status)
                  AND (:search IS NULL OR :search = '' OR
                       (col.name IS NOT NULL AND LOWER(col.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (cat.name IS NOT NULL AND LOWER(cat.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (ci.description IS NOT NULL AND LOWER(ci.description) LIKE LOWER(CONCAT('%', :search, '%'))))
            """
    )
    Page<CommissionRuleSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("collaboratorId") Long collaboratorId,
            @Param("categoryId") Long categoryId,
            @Param("catalogItemId") Long catalogItemId,
            @Param("status") CommissionRuleStatus status,
            Pageable pageable
    );
}
