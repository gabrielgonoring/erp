package company.aura.erp.dto.partner;

import company.aura.erp.entity.PartnerEntity;

public record PartnerSummaryResponse(
        Long id,
        String partnerIdentifier,
        String name,
        String document,
        String city,
        String state
) {
    public PartnerSummaryResponse(Long id, String name, String document, String city, String state) {
        this(id, null, name, document, city, state);
    }

    public static PartnerSummaryResponse fromEntity(PartnerEntity entity) {
        return new PartnerSummaryResponse(
                entity.getId(),
                entity.getPartnerIdentifier(),
                entity.getName(),
                entity.getDocument(),
                entity.getCity(),
                entity.getState()
        );
    }
}
