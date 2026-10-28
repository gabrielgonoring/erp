package company.aura.erp.repository;

import company.aura.erp.entity.PhysicalInvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicalInvoiceItemRepository extends JpaRepository<PhysicalInvoiceItemEntity, Long> {
}
