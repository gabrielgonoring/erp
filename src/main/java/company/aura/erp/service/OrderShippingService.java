package company.aura.erp.service;

import company.aura.erp.dto.order.*;
import company.aura.erp.entity.OrderEntity;
import company.aura.erp.entity.OrderShippingEntity;
import company.aura.erp.repository.OrderShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderShippingService {

    private final OrderShippingRepository orderShippingRepository;

    private boolean isShippingProvided(OrderCreateRequest request) {
        return request.getShipping() != null;
    }

    public Optional<OrderShippingEntity> findByOrderId(Long orderId) {
        log.info("findByOrderId() - START - orderId={}", orderId);
        Optional<OrderShippingEntity> shipping = orderShippingRepository.findByOrderId(orderId);
        log.info("findByOrderId() - END - orderId={}, isPresent={}", orderId, shipping.isPresent());
        return shipping;
    }

    @Transactional
    public void deleteShipping(Long orderId) {
        log.info("deleteShipping() - START - orderId={}", orderId);
        orderShippingRepository.deleteByOrderId(orderId);
        log.info("deleteShipping() - END - orderId={}", orderId);
    }

    @Transactional
    public OrderShippingEntity updateShipping(Long orderId, OrderShippingRequest request) {
        log.info("updateShipping() - START - orderId={}", orderId);
        if (request == null) {
            deleteShipping(orderId);
            log.info("updateShipping() - END - orderId={}, deleted=true", orderId);
            return null;
        }

        OrderShippingEntity shippingEntity = orderShippingRepository.findByOrderId(orderId)
                .orElseGet(() -> OrderShippingEntity.builder().orderId(orderId).build());

        if (request.getFreightMode() != null) {
            shippingEntity.setFreightMode(request.getFreightMode());
        }
        shippingEntity.setCarrierId(request.getCarrierId());
        shippingEntity.setVehiclePlate(request.getVehiclePlate());
        shippingEntity.setVehicleState(request.getVehicleState());
        shippingEntity.setRntrcCode(request.getRntrcCode());
        if (request.getPackageQuantity() != null) {
            shippingEntity.setPackageQuantity(request.getPackageQuantity());
        }
        if (request.getPackageSpecies() != null) {
            shippingEntity.setPackageSpecies(request.getPackageSpecies());
        }
        shippingEntity.setPackageBrand(request.getPackageBrand());
        shippingEntity.setPackageNumbering(request.getPackageNumbering());
        if (request.getGrossWeightKg() != null) {
            shippingEntity.setGrossWeightKg(request.getGrossWeightKg());
        }
        if (request.getNetWeightKg() != null) {
            shippingEntity.setNetWeightKg(request.getNetWeightKg());
        }
        shippingEntity.setTrackingCode(request.getTrackingCode());
        shippingEntity.setShippingNotes(request.getShippingNotes());

        OrderShippingEntity saved = orderShippingRepository.save(shippingEntity);
        log.info("updateShipping() - END - orderId={}, shippingId={}", orderId, saved.getId());
        return saved;
    }

    public OrderShippingEntity saveInitialShipping(OrderCreateRequest request, OrderEntity savedOrder) {
        Long orderId = savedOrder != null ? savedOrder.getId() : null;
        log.info("saveInitialShipping() - START - orderId={}", orderId);

        if (!isShippingProvided(request)) {
            log.info("saveInitialShipping() - END - orderId={}, shippingProvided=false", orderId);
            return null;
        }

        return saveInitialShipping(request.getShipping(), savedOrder);
    }

    public OrderShippingEntity saveInitialShipping(OrderShippingRequest shippingReq, OrderEntity savedOrder) {
        Long orderId = savedOrder != null ? savedOrder.getId() : null;
        log.info("saveInitialShipping() - START - orderId={}", orderId);

        if (shippingReq == null) {
            log.info("saveInitialShipping() - END - orderId={}, shippingProvided=false", orderId);
            return null;
        }

        int freightMode = shippingReq.getFreightMode() != null ? shippingReq.getFreightMode() : 9;

        OrderShippingEntity shippingEntity = OrderShippingEntity.builder()
                .orderId(savedOrder.getId())
                .carrierId(shippingReq.getCarrierId())
                .freightMode(freightMode)
                .vehiclePlate(shippingReq.getVehiclePlate())
                .vehicleState(shippingReq.getVehicleState())
                .rntrcCode(shippingReq.getRntrcCode())
                .packageQuantity(shippingReq.getPackageQuantity() != null ? shippingReq.getPackageQuantity() : 1)
                .packageSpecies(shippingReq.getPackageSpecies() != null ? shippingReq.getPackageSpecies() : "CAIXA")
                .packageBrand(shippingReq.getPackageBrand())
                .packageNumbering(shippingReq.getPackageNumbering())
                .grossWeightKg(shippingReq.getGrossWeightKg() != null ? shippingReq.getGrossWeightKg() : BigDecimal.ZERO)
                .netWeightKg(shippingReq.getNetWeightKg() != null ? shippingReq.getNetWeightKg() : BigDecimal.ZERO)
                .trackingCode(shippingReq.getTrackingCode())
                .shippingNotes(shippingReq.getShippingNotes())
                .build();

        OrderShippingEntity saved = orderShippingRepository.save(shippingEntity);
        log.info("saveInitialShipping() - END - orderId={}, shippingId={}", orderId, saved.getId());
        return saved;
    }

    public OrderShippingEntity upsertShippingOnClose(Long orderId, OrderCloseRequest request) {
        log.info("upsertShippingOnClose() - START - orderId={}", orderId);

        if (request.getShipping() == null) {
            orderShippingRepository.deleteByOrderId(orderId);
            log.info("upsertShippingOnClose() - END - orderId={}, deleted=true", orderId);
            return null;
        }

        OrderShippingEntity shippingEntity = orderShippingRepository.findByOrderId(orderId)
                .orElseGet(() -> OrderShippingEntity.builder().orderId(orderId).build());

        OrderShippingRequest shippingReq = request.getShipping();
        shippingEntity.setFreightMode(shippingReq.getFreightMode());
        shippingEntity.setCarrierId(shippingReq.getCarrierId());
        shippingEntity.setVehiclePlate(shippingReq.getVehiclePlate());
        shippingEntity.setVehicleState(shippingReq.getVehicleState());
        shippingEntity.setRntrcCode(shippingReq.getRntrcCode());
        shippingEntity.setPackageQuantity(shippingReq.getPackageQuantity());
        shippingEntity.setPackageSpecies(shippingReq.getPackageSpecies());
        shippingEntity.setPackageBrand(shippingReq.getPackageBrand());
        shippingEntity.setPackageNumbering(shippingReq.getPackageNumbering());
        shippingEntity.setGrossWeightKg(shippingReq.getGrossWeightKg());
        shippingEntity.setNetWeightKg(shippingReq.getNetWeightKg());
        shippingEntity.setTrackingCode(shippingReq.getTrackingCode());
        shippingEntity.setShippingNotes(shippingReq.getShippingNotes());

        OrderShippingEntity saved = orderShippingRepository.save(shippingEntity);
        log.info("upsertShippingOnClose() - END - orderId={}, shippingId={}", orderId, saved.getId());
        return saved;
    }

}
