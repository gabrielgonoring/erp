package company.aura.erp.repository;

import company.aura.erp.entity.ServiceInvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceInvoiceItemRepository extends JpaRepository<ServiceInvoiceItemEntity, Long> {
}
