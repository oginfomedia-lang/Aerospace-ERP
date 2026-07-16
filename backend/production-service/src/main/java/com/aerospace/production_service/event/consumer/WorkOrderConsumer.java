package com.aerospace.production_service.event.consumer;

import com.aerospace.production_service.event.KafkaTopics;
import com.aerospace.production_service.event.WorkOrderStatusChangedEvent;
import com.aerospace.production_service.service.ProductionOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

// Auto-creates a ProductionOrder + BOM the moment a WorkOrder's material is fully
// issued — see ProductionOrderServiceImpl.createFromWorkOrderIssuance.
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkOrderConsumer {

    private final ProductionOrderService productionOrderService;

    @KafkaListener(
        topics = KafkaTopics.WORK_ORDER_UPDATED,
        containerFactory = "workOrderStatusChangedKafkaListenerContainerFactory"
    )
    public void onWorkOrderStatusChanged(@Payload WorkOrderStatusChangedEvent event) {
        if (!"IN_PROGRESS".equals(event.getToStatus())) {
            return; // only the "all material issued" transition triggers auto-BOM creation
        }
        log.info("[KAFKA] WORK_ORDER_UPDATED (IN_PROGRESS) received — workOrderId={}, woNumber={}, lines={}",
                event.getWorkOrderId(), event.getWoNumber(),
                event.getMaterialLineItems() != null ? event.getMaterialLineItems().size() : 0);
        try {
            productionOrderService.createFromWorkOrderIssuance(event);
        } catch (Exception ex) {
            log.error("[CONSUMER] Failed to auto-create ProductionOrder for WO {}: {}",
                    event.getWoNumber(), ex.getMessage(), ex);
        }
    }
}
