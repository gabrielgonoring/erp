package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderShippingEntity;
import company.aura.erp.repository.OrderShippingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderShippingService {

    private final OrderShippingRepository orderShippingRepository;

    private boolean isShippingProvided(OrderCreateRequest request) {
        return request.shipping() != null;
    }

    public Optional<OrderShippingEntity> findByOrderId(Long orderId) {
        return orderShippingRepository.findByOrderId(orderId);
    }

    @Transactional
    public void deleteShipping(Long orderId) {
        orderShippingRepository.deleteByOrderId(orderId);
    }

    @Transactional
    public OrderShippingEntity updateShipping(Long orderId, OrderShippingRequest request) {
        if (request == null) {
            deleteShipping(orderId);
            return null;
        }

        OrderShippingEntity shippingEntity = orderShippingRepository.findByOrderId(orderId)
                .orElseGet(() -> OrderShippingEntity.builder().orderId(orderId).build());

        if (request.freightMode() != null) {
            shippingEntity.setFreightMode(request.freightMode());
        }
        shippingEntity.setCarrierId(request.carrierId());
        shippingEntity.setVehiclePlate(request.vehiclePlate());
        shippingEntity.setVehicleState(request.vehicleState());
        shippingEntity.setRntrcCode(request.rntrcCode());
        if (request.packageQuantity() != null) {
            shippingEntity.setPackageQuantity(request.packageQuantity());
        }
        if (request.packageSpecies() != null) {
            shippingEntity.setPackageSpecies(request.packageSpecies());
        }
        shippingEntity.setPackageBrand(request.packageBrand());
        shippingEntity.setPackageNumbering(request.packageNumbering());
        if (request.grossWeightKg() != null) {
            shippingEntity.setGrossWeightKg(request.grossWeightKg());
        }
        if (request.netWeightKg() != null) {
            shippingEntity.setNetWeightKg(request.netWeightKg());
        }
        shippingEntity.setTrackingCode(request.trackingCode());
        shippingEntity.setShippingNotes(request.shippingNotes());

        return orderShippingRepository.save(shippingEntity);
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
        shippingEntity.setVehicleState(shippingReq.vehicleState());
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
