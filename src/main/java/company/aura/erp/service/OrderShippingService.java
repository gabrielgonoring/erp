package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderItemEntity;
import company.aura.erp.entity.OrderShippingEntity;
import company.aura.erp.enums.CatalogItemType;
import company.aura.erp.enums.OrderStatus;
import company.aura.erp.repository.OrderRepository;
import company.aura.erp.repository.OrderShippingRepository;
import company.aura.erp.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderShippingService {

    private final OrderShippingRepository orderShippingRepository;


    private boolean isShippingProvided(OrderCreateRequest request) {
        return request.shipping() != null;
    }


    public OrderShippingEntity saveInitialShipping(OrderCreateRequest request, OrderEntity savedOrder) {
        if (!isShippingProvided(request)) {
            return null;
        }

        OrderShippingRequest shippingReq = request.shipping();

        int freightMode = (shippingReq != null && shippingReq.freightMode() != null) ? shippingReq.freightMode() : 9;

        OrderShippingEntity shippingEntity = OrderShippingEntity.builder()
                .orderId(savedOrder.getId())
                .carrierId(shippingReq != null ? shippingReq.carrierId() : null)
                .freightMode(freightMode)
                .vehiclePlate(shippingReq != null ? shippingReq.vehiclePlate() : null)
                .vehicleState(shippingReq != null ? shippingReq.vehicleState() : null)
                .rntrcCode(shippingReq != null ? shippingReq.rntrcCode() : null)
                .packageQuantity(shippingReq != null && shippingReq.packageQuantity() != null ? shippingReq.packageQuantity() : 1)
                .packageSpecies(shippingReq != null && shippingReq.packageSpecies() != null ? shippingReq.packageSpecies() : "CAIXA")
                .packageBrand(shippingReq != null ? shippingReq.packageBrand() : null)
                .packageNumbering(shippingReq != null ? shippingReq.packageNumbering() : null)
                .grossWeightKg(shippingReq != null && shippingReq.grossWeightKg() != null ? shippingReq.grossWeightKg() : BigDecimal.ZERO)
                .netWeightKg(shippingReq != null && shippingReq.netWeightKg() != null ? shippingReq.netWeightKg() : BigDecimal.ZERO)
                .trackingCode(shippingReq != null ? shippingReq.trackingCode() : null)
                .shippingNotes(shippingReq != null ? shippingReq.shippingNotes() : null)
                .build();

        return orderShippingRepository.save(shippingEntity);
    }


    public OrderShippingEntity upsertShippingOnClose(Long orderId, OrderCloseRequest request) {

        if (request.shipping() == null) {
            orderShippingRepository.deleteByOrderId(orderId);
            return null;
        }

        OrderShippingEntity shippingEntity = orderShippingRepository.findByOrderId(orderId)
                .orElseGet(() -> OrderShippingEntity.builder().orderId(orderId).build());

        OrderShippingRequest shippingReq = request.shipping();
        shippingEntity.setFreightMode(shippingReq.freightMode());
        shippingEntity.setCarrierId(shippingReq.carrierId());
        shippingEntity.setVehiclePlate(shippingReq.vehiclePlate());
        shippingEntity.setRntrcCode(shippingReq.rntrcCode());
        shippingEntity.setPackageQuantity(shippingReq.packageQuantity());
        shippingEntity.setPackageSpecies(shippingReq.packageSpecies());
        shippingEntity.setPackageBrand(shippingReq.packageBrand());
        shippingEntity.setPackageNumbering(shippingReq.packageNumbering());
        shippingEntity.setGrossWeightKg(shippingReq.grossWeightKg());
        shippingEntity.setNetWeightKg(shippingReq.netWeightKg());
        shippingEntity.setTrackingCode(shippingReq.trackingCode());
        shippingEntity.setShippingNotes(shippingReq.shippingNotes());


        return orderShippingRepository.save(shippingEntity);
    }

}
