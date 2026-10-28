package company.aura.erp.repository;

import company.aura.erp.entity.InvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity, Long> {

    List<InvoiceItemEntity> findByInvoiceId(Long invoiceId);
}
