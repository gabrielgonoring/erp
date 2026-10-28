package company.aura.erp.repository;

import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    List<InvoiceEntity> findByCompanyId(Long companyId);

    Optional<InvoiceEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<InvoiceEntity> findByCompanyIdAndStatus(Long companyId, String status);

    List<InvoiceEntity> findByCompanyIdAndDocumentType(Long companyId, DocumentType documentType);

    Optional<InvoiceEntity> findByCompanyIdAndAccessKey(Long companyId, String accessKey);
}
