package company.aura.erp.specification;

import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.enums.DocumentType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InvoiceSpecification {

    public static Specification<InvoiceEntity> filter(
            Long companyId,
            List<Long> partnerIds,
            String externalReference,
            Integer operationType,
            DocumentType documentType,
            String status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyId != null) {
                predicates.add(cb.equal(root.get("companyId"), companyId));
            }

            if (partnerIds != null && !partnerIds.isEmpty()) {
                List<Long> cleanPartnerIds = partnerIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                if (!cleanPartnerIds.isEmpty()) {
                    predicates.add(root.get("partnerId").in(cleanPartnerIds));
                }
            }

            if (externalReference != null && !externalReference.isBlank()) {
                predicates.add(cb.equal(root.get("externalReference"), externalReference.trim()));
            }

            if (operationType != null) {
                predicates.add(cb.equal(root.get("operationType"), operationType));
            }

            if (documentType != null) {
                predicates.add(cb.equal(root.get("documentType"), documentType));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
