package company.aura.erp.mapper;

import company.aura.erp.dto.order.OrderInstallmentResponse;
import company.aura.erp.dto.order.OrderInvoiceResponse;
import company.aura.erp.dto.order.OrderItemResponse;
import company.aura.erp.dto.order.OrderResponse;
import company.aura.erp.dto.order.OrderShippingResponse;
import company.aura.erp.dto.order.PaymentTransactionResponse;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderInstallmentEntity;
import company.aura.erp.entity.OrderInvoiceEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.entity.OrderShippingEntity;
import company.aura.erp.entity.PaymentTransactionEntity;

import java.util.Collections;
import java.util.List;

public class OrderMapper {

    public static OrderResponse fromEntity(
            OrderEntity order,
            List<OrderItemEntity> items,
            List<OrderInstallmentEntity> installments
    ) {
        return fromEntity(order, null, items, installments, null, Collections.emptyList());
    }

    public static OrderResponse fromEntity(
            OrderEntity order,
            List<OrderItemEntity> items,
            List<OrderInstallmentEntity> installments,
            OrderShippingEntity shipping
    ) {
        return fromEntity(order, null, items, installments, shipping, Collections.emptyList());
    }

    public static OrderResponse fromEntity(
            OrderEntity order,
            String customerName,
            List<OrderItemEntity> items,
            List<OrderInstallmentEntity> installments,
            OrderShippingEntity shipping
    ) {
        return fromEntity(order, customerName, items, installments, shipping, Collections.emptyList());
    }

    public static OrderResponse fromEntity(
            OrderEntity order,
            String customerName,
            List<OrderItemEntity> items,
            List<OrderInstallmentEntity> installments,
            OrderShippingEntity shipping,
            List<OrderInvoiceEntity> invoices
    ) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = items != null
                ? items.stream().map(OrderItemMapper::fromEntity).toList()
                : Collections.emptyList();

        List<OrderInstallmentResponse> installmentResponses = installments != null
                ? installments.stream().map(OrderInstallmentMapper::fromEntity).toList()
                : Collections.emptyList();

        OrderShippingResponse shippingResponse = OrderShippingMapper.fromEntity(shipping);

        List<OrderInvoiceResponse> invoiceResponses = invoices != null
                ? invoices.stream().map(OrderInvoiceMapper::fromEntity).toList()
                : Collections.emptyList();

        return OrderResponse.builder()
                .id(order.getId())
                .companyId(order.getCompanyId())
                .partnerId(order.getPartnerId())
                .customerName(customerName)
                .status(order.getStatus())
                .servicesAmount(order.getServicesAmount())
                .productsAmount(order.getProductsAmount())
                .freightAmount(order.getFreightAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .completedAt(order.getCompletedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .installments(installmentResponses)
                .shipping(shippingResponse)
                .invoices(invoiceResponses)
                .build();
    }

    public static OrderResponse fromEntityWithResponses(
            OrderEntity order,
            List<OrderItemEntity> items,
            List<OrderInstallmentResponse> installments,
            OrderShippingEntity shipping
    ) {
        return fromEntityWithResponses(order, null, items, installments, shipping, Collections.emptyList());
    }

    public static OrderResponse fromEntityWithResponses(
            OrderEntity order,
            String customerName,
            List<OrderItemEntity> items,
            List<OrderInstallmentResponse> installments,
            OrderShippingEntity shipping
    ) {
        return fromEntityWithResponses(order, customerName, items, installments, shipping, Collections.emptyList());
    }

    public static OrderResponse fromEntityWithResponses(
            OrderEntity order,
            String customerName,
            List<OrderItemEntity> items,
            List<OrderInstallmentResponse> installments,
            OrderShippingEntity shipping,
            List<OrderInvoiceResponse> invoices
    ) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = items != null
                ? items.stream().map(OrderItemMapper::fromEntity).toList()
                : Collections.emptyList();

        OrderShippingResponse shippingResponse = OrderShippingMapper.fromEntity(shipping);

        return OrderResponse.builder()
                .id(order.getId())
                .companyId(order.getCompanyId())
                .partnerId(order.getPartnerId())
                .customerName(customerName)
                .status(order.getStatus())
                .servicesAmount(order.getServicesAmount())
                .productsAmount(order.getProductsAmount())
                .freightAmount(order.getFreightAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .completedAt(order.getCompletedAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .installments(installments != null ? installments : Collections.emptyList())
                .shipping(shippingResponse)
                .invoices(invoices != null ? invoices : Collections.emptyList())
                .build();
    }

    public static OrderInvoiceResponse fromEntity(OrderInvoiceEntity entity) {
        return OrderInvoiceMapper.fromEntity(entity);
    }

    public static OrderItemResponse fromEntity(OrderItemEntity entity) {
        return OrderItemMapper.fromEntity(entity);
    }

    public static OrderShippingResponse fromEntity(OrderShippingEntity entity) {
        return OrderShippingMapper.fromEntity(entity);
    }

    public static OrderInstallmentResponse fromEntity(OrderInstallmentEntity entity) {
        return OrderInstallmentMapper.fromEntity(entity);
    }

    public static OrderInstallmentResponse fromEntity(OrderInstallmentEntity entity, List<PaymentTransactionEntity> transactions) {
        return OrderInstallmentMapper.fromEntity(entity, transactions);
    }

    public static PaymentTransactionResponse fromEntity(PaymentTransactionEntity entity) {
        return PaymentTransactionMapper.fromEntity(entity);
    }
}
