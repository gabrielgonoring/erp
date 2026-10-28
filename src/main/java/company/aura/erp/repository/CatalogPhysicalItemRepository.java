package company.aura.erp.repository;

import company.aura.erp.entity.CatalogPhysicalItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogPhysicalItemRepository extends JpaRepository<CatalogPhysicalItemEntity, Long> {
}
