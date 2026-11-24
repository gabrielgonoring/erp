package company.aura.erp.repository;

import company.aura.erp.dto.payable.PayableSummaryResponse;
import company.aura.erp.entity.PayableEntity;
import company.aura.erp.enums.PayableStatus;
import company.aura.erp.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayableRepository extends JpaRepository<PayableEntity, Long> {

    Optional<PayableEntity> findByIdAndCompanyId(Long id, Long companyId);

    @Query(
            value = """
                SELECT new company.aura.erp.dto.payable.PayableSummaryResponse(
                    py.id,
                    py.originType,
                    py.originId,
                    py.description,
                    p.id,
                    p.document,
                    p.name,
                    c.id,
                    c.cpf,
                    c.name,
                    py.amount,
                    py.pendingAmount,
                    py.status,
                    py.paymentMethod,
                    py.dueDate
                )
                FROM PayableEntity py
                LEFT JOIN PartnerEntity p ON py.partnerId = p.id
                LEFT JOIN CollaboratorEntity c ON py.collaboratorId = c.id
                WHERE py.companyId = :companyId
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(py.description) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       (p.name IS NOT NULL AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (c.name IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (c.cpf IS NOT NULL AND LOWER(c.cpf) LIKE LOWER(CONCAT('%', :search, '%'))))
                  AND (:hasDateRange = false OR py.dueDate BETWEEN :startDate AND :endDate)
                  AND (:hasStartDateOnly = false OR py.dueDate >= :startDate)
                  AND (:hasEndDateOnly = false OR py.dueDate <= :endDate)
                  AND (:hasStatuses = false OR py.status IN (:statuses))
                  AND (:hasPaymentMethods = false OR py.paymentMethod IN (:paymentMethods))
            """,
            countQuery = """
                SELECT count(py)
                FROM PayableEntity py
                LEFT JOIN PartnerEntity p ON py.partnerId = p.id
                LEFT JOIN CollaboratorEntity c ON py.collaboratorId = c.id
                WHERE py.companyId = :companyId
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(py.description) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       (p.name IS NOT NULL AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (p.document IS NOT NULL AND LOWER(p.document) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (c.name IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))) OR
                       (c.cpf IS NOT NULL AND LOWER(c.cpf) LIKE LOWER(CONCAT('%', :search, '%'))))
                  AND (:hasDateRange = false OR py.dueDate BETWEEN :startDate AND :endDate)
                  AND (:hasStartDateOnly = false OR py.dueDate >= :startDate)
                  AND (:hasEndDateOnly = false OR py.dueDate <= :endDate)
                  AND (:hasStatuses = false OR py.status IN (:statuses))
                  AND (:hasPaymentMethods = false OR py.paymentMethod IN (:paymentMethods))
            """
    )
    Page<PayableSummaryResponse> searchSummarized(
            @Param("companyId") Long companyId,
            @Param("search") String search,
            @Param("hasDateRange") boolean hasDateRange,
            @Param("hasStartDateOnly") boolean hasStartDateOnly,
            @Param("hasEndDateOnly") boolean hasEndDateOnly,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("hasStatuses") boolean hasStatuses,
            @Param("statuses") List<PayableStatus> statuses,
            @Param("hasPaymentMethods") boolean hasPaymentMethods,
            @Param("paymentMethods") List<PaymentMethod> paymentMethods,
            Pageable pageable
    );
}
