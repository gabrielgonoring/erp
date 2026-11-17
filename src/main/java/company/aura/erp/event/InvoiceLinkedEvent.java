package company.aura.erp.event;

public record InvoiceLinkedEvent(
        Long companyId,
        Long orderId,
        Long invoiceId
) {
}
