package company.aura.erp.repository;

import company.aura.erp.entity.CatalogServiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogServiceItemRepository extends JpaRepository<CatalogServiceItemEntity, Long> {
}
