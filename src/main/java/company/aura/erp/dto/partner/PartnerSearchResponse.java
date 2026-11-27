package company.aura.erp.dto.partner;

public record PartnerSearchResponse(
        Long id,
        String partnerIdentifier,
        String name,
        String document
) {
}
