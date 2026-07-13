package com.aerospace.inventory_service.event.consumer;

import com.aerospace.inventory_service.entity.Part;
import com.aerospace.inventory_service.entity.StockBalance;
import com.aerospace.inventory_service.entity.StockStatus;
import com.aerospace.inventory_service.event.KafkaTopics;
import com.aerospace.inventory_service.event.MaterialShortageEvent;
import com.aerospace.inventory_service.event.WorkOrderCreatedEvent;
import com.aerospace.inventory_service.event.producer.InventoryProducer;
import com.aerospace.inventory_service.repository.PartRepository;
import com.aerospace.inventory_service.repository.StockBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

// When a Work Order is created, check each material line's required quantity against
// what's currently APPROVED in stock. A shortfall publishes MATERIAL_SHORTAGE, which
// procurement-service consumes to auto-raise a DRAFT PO. Kafka-driven, not a synchronous
// callback, to stay consistent with the rest of this codebase (no cross-service REST).
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkOrderShortageConsumer {

    private final PartRepository partRepository;
    private final StockBalanceRepository balanceRepository;
    private final InventoryProducer inventoryProducer;

    @KafkaListener(
        topics = KafkaTopics.WORK_ORDER_CREATED,
        containerFactory = "workOrderCreatedKafkaListenerContainerFactory"
    )
    public void consumeWorkOrderCreated(@Payload WorkOrderCreatedEvent event) {
        if (event.getMaterialLines() == null || event.getMaterialLines().isEmpty()) {
            return;
        }

        for (WorkOrderCreatedEvent.MaterialLineSummary line : event.getMaterialLines()) {
            if (line.getPartId() == null || line.getQuantityRequired() == null) continue;

            Part part = partRepository.findById(line.getPartId()).orElse(null);
            if (part == null) {
                log.warn("[SHORTAGE-CHECK] WO {} references unknown partId={} — skipping",
                        event.getWoNumber(), line.getPartId());
                continue;
            }

            int available = balanceRepository.findAllBalancesByPart(part.getId()).stream()
                    .filter(b -> b.getStatus() == StockStatus.APPROVED)
                    .mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0)
                    .sum();

            int required = line.getQuantityRequired();
            if (required <= available) continue;

            int shortfall = required - available;
            log.warn("[SHORTAGE-CHECK] WO {} needs {} of part {} but only {} APPROVED — shortfall {}",
                    event.getWoNumber(), required, part.getSkuCode(), available, shortfall);

            try {
                inventoryProducer.sendMaterialShortageEvent(MaterialShortageEvent.builder()
                        .partId(part.getId())
                        .skuCode(part.getSkuCode())
                        .partName(part.getName())
                        .requiredQty(required)
                        .availableQty(available)
                        .shortfallQty(shortfall)
                        .preferredSupplierId(part.getPreferredSupplierId())
                        .workOrderId(event.getWorkOrderId())
                        .woNumber(event.getWoNumber())
                        .build());
            } catch (Exception ex) {
                log.warn("[KAFKA] Failed to publish MATERIAL_SHORTAGE for partId={}: {}",
                        part.getId(), ex.getMessage());
            }
        }
    }
}
