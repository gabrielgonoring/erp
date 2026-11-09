package company.aura.erp.repository;

import company.aura.erp.entity.OrderInvoiceEntity;
import company.aura.erp.enums.InvoicePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderInvoiceRepository extends JpaRepository<OrderInvoiceEntity, Long> {

    List<OrderInvoiceEntity> findByOrderId(Long orderId);

    List<OrderInvoiceEntity> findByCompanyIdAndOrderId(Long companyId, Long orderId);

    List<OrderInvoiceEntity> findByCompanyIdAndOrderIdAndInvoiceId(Long companyId, Long orderId, Long invoiceId);

    Optional<OrderInvoiceEntity> findByOrderIdAndInvoiceIdAndInvoicePurpose(Long orderId, Long invoiceId, InvoicePurpose invoicePurpose);

    boolean existsByOrderIdAndInvoiceIdAndInvoicePurpose(Long orderId, Long invoiceId, InvoicePurpose invoicePurpose);

    void deleteByOrderId(Long orderId);

    void deleteByCompanyIdAndOrderIdAndInvoiceId(Long companyId, Long orderId, Long invoiceId);
}
