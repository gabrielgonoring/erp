package company.aura.erp.dto.order;

import company.aura.erp.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long companyId;
    private Long partnerId;
    private String customerName;
    private OrderStatus status;
    private BigDecimal servicesAmount;
    private BigDecimal productsAmount;
    private BigDecimal freightAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
    private List<OrderInstallmentResponse> installments;
    private OrderShippingResponse shipping;
    private List<OrderInvoiceResponse> invoices;

    public OrderResponse(
            Long id,
            Long companyId,
            Long partnerId,
            OrderStatus status,
            BigDecimal servicesAmount,
            BigDecimal productsAmount,
            BigDecimal freightAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String notes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items,
            List<OrderInstallmentResponse> installments
    ) {
        this(id, companyId, partnerId, null, status,
                servicesAmount, productsAmount, freightAmount, discountAmount,
                totalAmount, notes, completedAt, createdAt, updatedAt, items, installments, null, Collections.emptyList());
    }

    public OrderResponse(
            Long id,
            Long companyId,
            Long partnerId,
            String customerName,
            OrderStatus status,
            BigDecimal servicesAmount,
            BigDecimal productsAmount,
            BigDecimal freightAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String notes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items,
            List<OrderInstallmentResponse> installments
    ) {
        this(id, companyId, partnerId, customerName, status,
                servicesAmount, productsAmount, freightAmount, discountAmount,
                totalAmount, notes, completedAt, createdAt, updatedAt, items, installments, null, Collections.emptyList());
    }

    public OrderResponse(
            Long id,
            Long companyId,
            Long partnerId,
            OrderStatus status,
            BigDecimal servicesAmount,
            BigDecimal productsAmount,
            BigDecimal freightAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String notes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items,
            List<OrderInstallmentResponse> installments,
            OrderShippingResponse shipping
    ) {
        this(id, companyId, partnerId, null, status,
                servicesAmount, productsAmount, freightAmount, discountAmount,
                totalAmount, notes, completedAt, createdAt, updatedAt, items, installments, shipping, Collections.emptyList());
    }

    public OrderResponse(
            Long id,
            Long companyId,
            Long partnerId,
            String customerName,
            OrderStatus status,
            BigDecimal servicesAmount,
            BigDecimal productsAmount,
            BigDecimal freightAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String notes,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items,
            List<OrderInstallmentResponse> installments,
            OrderShippingResponse shipping
    ) {
        this(id, companyId, partnerId, customerName, status,
                servicesAmount, productsAmount, freightAmount, discountAmount,
                totalAmount, notes, completedAt, createdAt, updatedAt, items, installments, shipping, Collections.emptyList());
    }
}
