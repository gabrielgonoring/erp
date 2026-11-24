package company.aura.erp.repository;

import company.aura.erp.dto.catalog.CatalogCategorySummaryResponse;
import company.aura.erp.entity.CatalogCategoryEntity;
import company.aura.erp.enums.CatalogCategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatalogCategoryRepository extends JpaRepository<CatalogCategoryEntity, Long> {

    Optional<CatalogCategoryEntity> findByIdAndCompanyIdAndStatusNot(Long id, Long companyId, CatalogCategoryStatus status);

    @Query("SELECT c.name FROM CatalogCategoryEntity c WHERE c.id = :id AND c.companyId = :companyId")
    Optional<String> findNameByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    boolean existsByCompanyIdAndNameIgnoreCaseAndStatusNot(Long companyId, String name, CatalogCategoryStatus status);

    boolean existsByCompanyIdAndNameIgnoreCaseAndStatusNotAndIdNot(Long companyId, String name, CatalogCategoryStatus status, Long id);

    @Query(
            value = "SELECT new company.aura.erp.dto.catalog.CatalogCategorySummaryResponse(c.id, c.name, c.status) " +
                    "FROM CatalogCategoryEntity c " +
                    "WHERE c.companyId = :companyId " +
                    "AND (:search IS NULL OR :search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR c.status = :status)",
            countQuery = "SELECT count(c) " +
                    "FROM CatalogCategoryEntity c " +
                    "WHERE c.companyId = :companyId " +
                    "AND (:search IS NULL OR :search = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:status IS NULL OR c.status = :status)"
    )
    Page<CatalogCategorySummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("status") CatalogCategoryStatus status,
            Pageable pageable
    );
}
