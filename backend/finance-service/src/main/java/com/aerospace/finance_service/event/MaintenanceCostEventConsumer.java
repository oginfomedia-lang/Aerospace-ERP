package com.aerospace.finance_service.event;

import com.aerospace.finance_service.entity.PostingTriggerType;
import com.aerospace.finance_service.service.PostingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaintenanceCostEventConsumer {

    private final PostingEngine postingEngine;

    @KafkaListener(topics = "maintenance-cost-events", groupId = "finance-service")
    public void onMaintenanceCost(MaintenanceCostEvent event) {
        if (event.getTotalCost() == null) {
            log.warn("MaintenanceCostEvent for {} has no cost — skipping", event.getMaintenanceOrderRef());
            return;
        }
        log.info("Received MaintenanceCostEvent for order {}", event.getMaintenanceOrderRef());
        try {
            postingEngine.postFromEvent(PostingTriggerType.MAINTENANCE_COST,
                    event.getTotalCost(), event.getCurrency(), "MAINTENANCE_COST",
                    event.getMaintenanceOrderRef(), event.getCostCenterId(), null);
        } catch (Exception ex) {
            log.error("Failed to post journal for MaintenanceCostEvent ref={}: {}",
                    event.getMaintenanceOrderRef(), ex.getMessage(), ex);
        }
    }
}
