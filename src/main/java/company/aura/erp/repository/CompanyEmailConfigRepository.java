package company.aura.erp.repository;

import company.aura.erp.entity.CompanyEmailConfigEntity;
import company.aura.erp.enums.CompanyEmailConfigStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyEmailConfigRepository extends JpaRepository<CompanyEmailConfigEntity, Long> {

    Optional<CompanyEmailConfigEntity> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndNotificationType(Long companyId, String notificationType);

    boolean existsByCompanyIdAndNotificationTypeAndIdNot(Long companyId, String notificationType, Long id);

    @Query("""
            SELECT c FROM CompanyEmailConfigEntity c
            WHERE c.companyId = :companyId
              AND (:status IS NULL OR c.status = :status)
              AND (:notificationType IS NULL OR :notificationType = '' OR c.notificationType = :notificationType)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(c.fromEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.fromName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   (c.replyTo IS NOT NULL AND LOWER(c.replyTo) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                   LOWER(c.notificationType) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<CompanyEmailConfigEntity> searchByCompanyId(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("notificationType") String notificationType,
            @Param("status") CompanyEmailConfigStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM CompanyEmailConfigEntity c
            WHERE c.companyId = :companyId
              AND c.status = :status
              AND c.notificationType in :types
            """)
    List<CompanyEmailConfigEntity> findActiveConfigsForNotification(
            @Param("companyId") Long companyId,
            @Param("types") List<String> types,
            @Param("status") CompanyEmailConfigStatus status
    );
}
