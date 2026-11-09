package company.aura.erp.repository;

import company.aura.erp.entity.CollaboratorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollaboratorRepository extends JpaRepository<CollaboratorEntity, Long> {

    Optional<CollaboratorEntity> findByIdAndCompanyId(Long id, Long companyId);

    Optional<CollaboratorEntity> findByIdAndCompanyIdAndStatusNot(Long id, Long companyId, String status);

    List<CollaboratorEntity> findByCompanyId(Long companyId);

    boolean existsByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT c FROM CollaboratorEntity c WHERE c.companyId = :companyId " +
           "AND (:status IS NULL OR :status = '' OR UPPER(c.status) = UPPER(:status)) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.cpf LIKE CONCAT('%', :search, '%'))")
    Page<CollaboratorEntity> searchByCompanyIdAndTerm(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

}
