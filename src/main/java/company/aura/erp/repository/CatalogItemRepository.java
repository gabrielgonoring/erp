package company.aura.erp.repository;

import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.enums.CatalogItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItemEntity, Long> {

    List<CatalogItemEntity> findByCompanyId(Long companyId);

    Optional<CatalogItemEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<CatalogItemEntity> findByCompanyIdAndType(Long companyId, CatalogItemType type);

    List<CatalogItemEntity> findByCompanyIdAndDeletedAtIsNull(Long companyId);

    @Query("SELECT c FROM CatalogItemEntity c " +
           "LEFT JOIN FETCH c.physicalItem " +
           "LEFT JOIN FETCH c.serviceItem " +
           "WHERE c.id = :id AND c.companyId = :companyId AND (c.status IS NULL OR c.status <> :status)")
    Optional<CatalogItemEntity> findByIdAndCompanyIdAndStatusNot(
            @Param("id") Long id,
            @Param("companyId") Long companyId,
            @Param("status") String status
    );

    @Query("SELECT c FROM CatalogItemEntity c WHERE c.companyId = :companyId AND (c.status IS NULL OR c.status <> 'DELETED') AND c.deletedAt IS NULL AND " +
           "(:search IS NULL OR :search = '' OR " +
           "CAST(c.id AS string) LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(c.internalCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CatalogItemEntity> searchByCompanyIdAndTerm(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            Pageable pageable
    );
}
