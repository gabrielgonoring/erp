package company.aura.erp.repository;

import company.aura.erp.dto.partner.PartnerSearchResponse;
import company.aura.erp.entity.PartnerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {

    Optional<PartnerEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT p.name FROM PartnerEntity p WHERE p.id = :id AND p.companyId = :companyId")
    Optional<String> findNameByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    boolean existsByIdAndCompanyId(Long id, Long companyId);

    Optional<PartnerEntity> findByIdAndCompanyIdAndStatusNot(Long id, Long companyId, String status);

    @Query("SELECT p FROM PartnerEntity p " +
           "WHERE p.id = :id " +
           "AND p.companyId = :companyId " +
           "AND (p.status IS NULL OR p.status <> 'DELETED') " +
           "AND p.deletedAt IS NULL")
    Optional<PartnerEntity> findActiveByIdAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId
    );

    @Query("SELECT p FROM PartnerEntity p " +
           "WHERE p.companyId = :companyId " +
           "AND p.document = :document " +
           "AND (p.status IS NULL OR p.status <> 'DELETED') " +
           "AND p.deletedAt IS NULL " +
           "ORDER BY p.id ASC")
    List<PartnerEntity> findActiveByCompanyIdAndDocument(
            @Param("companyId") Long companyId,
            @Param("document") String document
    );

    @Query("SELECT p FROM PartnerEntity p WHERE p.companyId = :companyId AND (p.status IS NULL OR p.status <> 'DELETED') AND p.deletedAt IS NULL AND " +
           "(:isCustomer IS NULL OR COALESCE(p.isCustomer, false) = :isCustomer) AND " +
           "(:isSupplier IS NULL OR COALESCE(p.isSupplier, false) = :isSupplier) AND " +
           "(:isCarrier IS NULL OR COALESCE(p.isCarrier, false) = :isCarrier) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(p.partnerIdentifier IS NOT NULL AND LOWER(p.partnerIdentifier) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(p.city IS NOT NULL AND LOWER(p.city) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<PartnerEntity> searchByCompanyIdAndTerm(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("isCustomer") Boolean isCustomer,
            @Param("isSupplier") Boolean isSupplier,
            @Param("isCarrier") Boolean isCarrier,
            Pageable pageable
    );

    @Query(
            value = "SELECT new company.aura.erp.dto.partner.PartnerSearchResponse(" +
                    "p.id, p.partnerIdentifier, p.name, p.document) " +
                    "FROM PartnerEntity p " +
                    "WHERE p.companyId = :companyId AND p.status = 'ACTIVE' AND p.deletedAt IS NULL AND " +
                    "(:isCustomer IS NULL OR p.isCustomer = :isCustomer OR (:isCustomer = false AND p.isCustomer IS NULL)) AND " +
                    "(:isCarrier IS NULL OR p.isCarrier = :isCarrier) AND " +
                    "(:isSupplier IS NULL OR p.isSupplier = :isSupplier OR (:isSupplier = false AND p.isSupplier IS NULL))",
            countQuery = "SELECT count(p) " +
                    "FROM PartnerEntity p " +
                    "WHERE p.companyId = :companyId AND p.status = 'ACTIVE' AND p.deletedAt IS NULL AND " +
                    "(:isCustomer IS NULL OR p.isCustomer = :isCustomer OR (:isCustomer = false AND p.isCustomer IS NULL)) AND " +
                    "(:isCarrier IS NULL OR p.isCarrier = :isCarrier) AND " +
                    "(:isSupplier IS NULL OR p.isSupplier = :isSupplier OR (:isSupplier = false AND p.isSupplier IS NULL))"
    )
    Page<PartnerSearchResponse> findActiveSimplifiedByCompanyId(
            @Param("companyId") Long companyId,
            @Param("isCustomer") Boolean isCustomer,
            @Param("isCarrier") Boolean isCarrier,
            @Param("isSupplier") Boolean isSupplier,
            Pageable pageable
    );

    @Query(
            value = "SELECT new company.aura.erp.dto.partner.PartnerSearchResponse(" +
                    "p.id, p.partnerIdentifier, p.name, p.document) " +
                    "FROM PartnerEntity p " +
                    "WHERE p.companyId = :companyId AND p.status = 'ACTIVE' AND p.deletedAt IS NULL AND " +
                    "(:isCustomer IS NULL OR p.isCustomer = :isCustomer OR (:isCustomer = false AND p.isCustomer IS NULL)) AND " +
                    "(:isCarrier IS NULL OR p.isCarrier = :isCarrier) AND " +
                    "(:isSupplier IS NULL OR p.isSupplier = :isSupplier OR (:isSupplier = false AND p.isSupplier IS NULL)) AND (" +
                    "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.partnerIdentifier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT count(p) " +
                    "FROM PartnerEntity p " +
                    "WHERE p.companyId = :companyId AND p.status = 'ACTIVE' AND p.deletedAt IS NULL AND " +
                    "(:isCustomer IS NULL OR p.isCustomer = :isCustomer OR (:isCustomer = false AND p.isCustomer IS NULL)) AND " +
                    "(:isCarrier IS NULL OR p.isCarrier = :isCarrier) AND " +
                    "(:isSupplier IS NULL OR p.isSupplier = :isSupplier OR (:isSupplier = false AND p.isSupplier IS NULL)) AND (" +
                    "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.partnerIdentifier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%')))"
    )
    Page<PartnerSearchResponse> searchSimplified(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("isCustomer") Boolean isCustomer,
            @Param("isCarrier") Boolean isCarrier,
            @Param("isSupplier") Boolean isSupplier,
            Pageable pageable
    );
}
