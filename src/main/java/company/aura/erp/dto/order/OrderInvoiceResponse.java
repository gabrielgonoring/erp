package company.aura.erp.dto.order;

import company.aura.erp.entity.OrderInvoiceEntity;
import company.aura.erp.enums.InvoicePurpose;

import java.time.LocalDateTime;

public record OrderInvoiceResponse(
        Long id,
        Long companyId,
        Long orderId,
        Long invoiceId,
        InvoicePurpose invoicePurpose,
        String notes,
        LocalDateTime createdAt
) {
    public static OrderInvoiceResponse fromEntity(OrderInvoiceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderInvoiceResponse(
                entity.getId(),
                entity.getCompanyId(),
                entity.getOrderId(),
                entity.getInvoiceId(),
                entity.getInvoicePurpose(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}
