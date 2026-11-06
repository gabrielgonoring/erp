package company.aura.erp.mapper;

import company.aura.erp.dto.order.OrderInvoiceResponse;
import company.aura.erp.entity.OrderInvoiceEntity;

public class OrderInvoiceMapper {

    public static OrderInvoiceResponse fromEntity(OrderInvoiceEntity entity) {
        if (entity == null) {
            return null;
        }
        return OrderInvoiceResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .orderId(entity.getOrderId())
                .invoiceId(entity.getInvoiceId())
                .invoicePurpose(entity.getInvoicePurpose())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
