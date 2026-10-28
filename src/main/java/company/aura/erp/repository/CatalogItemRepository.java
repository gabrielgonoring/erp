package company.aura.erp.repository;

import company.aura.erp.entity.CatalogItemEntity;
import company.aura.erp.enums.CatalogItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItemEntity, Long> {

    List<CatalogItemEntity> findByCompanyId(Long companyId);

    Optional<CatalogItemEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<CatalogItemEntity> findByCompanyIdAndType(Long companyId, CatalogItemType type);

    List<CatalogItemEntity> findByCompanyIdAndDeletedAtIsNull(Long companyId);
}
