package com.aerospace.production_service.event.consumer;

import com.aerospace.production_service.entity.ProductionMaterial;
import com.aerospace.production_service.entity.ProductionOrder;
import com.aerospace.production_service.event.KafkaTopics;
import com.aerospace.production_service.event.MaterialIssuedEvent;
import com.aerospace.production_service.repository.ProductionOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductionConsumer {

    private final ProductionOrderRepository productionOrderRepository;

    // Listens to inventory-service MATERIAL_ISSUED events.
    // Use this to track which parts have been drawn for a production order.
    @KafkaListener(
        topics = KafkaTopics.MATERIAL_ISSUED,
        containerFactory = "materialIssuedKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeMaterialIssued(@Payload MaterialIssuedEvent event) {
        log.info("[KAFKA] MATERIAL_ISSUED received — partId={}, batch={}, qty={}, issuedTo={}",
                event.getPartId(), event.getBatchNumber(),
                event.getQuantityIssued(), event.getIssuedTo());

        if (event.getWorkOrderId() == null) {
            return; // ad-hoc issue, not tied to any Work Order / production order
        }

        for (ProductionOrder order : productionOrderRepository.findByWorkOrderId(event.getWorkOrderId())) {
            order.getMaterials().stream()
                    .filter(m -> m.getPartId() != null && m.getPartId().equals(event.getPartId()))
                    .filter(m -> m.getStatus() == ProductionMaterial.MaterialStatus.PENDING)
                    .findFirst()
                    .ifPresent(material -> {
                        int alreadyConsumed = material.getQuantityConsumed() != null ? material.getQuantityConsumed() : 0;
                        int newTotalConsumed = alreadyConsumed + event.getQuantityIssued();
                        material.setQuantityConsumed(newTotalConsumed);
                        material.setBatchNumber(event.getBatchNumber());
                        if (newTotalConsumed >= material.getQuantityRequired()) {
                            material.setStatus(ProductionMaterial.MaterialStatus.CONSUMED);
                        }
                        productionOrderRepository.save(order);
                        log.info("[CONSUMER] MATERIAL_ISSUED → ProductionOrder={} partId={} consumed {}/{}",
                                order.getPoNumber(), event.getPartId(), newTotalConsumed, material.getQuantityRequired());
                    });
        }
    }

    // Listens to inventory-service STOCK_LOW events.
    // Use this to put related production orders ON_HOLD automatically.
    @KafkaListener(
        topics = KafkaTopics.STOCK_LOW,
        groupId = "production-stock-alert-group"
    )
    public void consumeStockLow(@Payload String rawEvent) {
        log.warn("[KAFKA] STOCK_LOW alert received — {}", rawEvent);
        // Future: parse event, find production orders using that partId,
        // flag them ON_HOLD and publish ProductionOrderOnHoldEvent.
    }
}
