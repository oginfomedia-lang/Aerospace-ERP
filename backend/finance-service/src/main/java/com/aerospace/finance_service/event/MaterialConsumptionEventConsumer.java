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
public class MaterialConsumptionEventConsumer {

    private final PostingEngine postingEngine;

    @KafkaListener(topics = "material-consumption-events", groupId = "finance-service")
    public void onMaterialConsumption(MaterialConsumptionEvent event) {
        if (event.getTotalCost() == null) {
            log.warn("MaterialConsumptionEvent for {} has no cost — skipping", event.getConsumptionRef());
            return;
        }
        log.info("Received MaterialConsumptionEvent for ref {}", event.getConsumptionRef());
        try {
            postingEngine.postFromEvent(PostingTriggerType.MATERIAL_CONSUMPTION,
                    event.getTotalCost(), event.getCurrency(), "MATERIAL_CONSUMPTION",
                    event.getConsumptionRef(), event.getCostCenterId(), null);
        } catch (Exception ex) {
            log.error("Failed to post journal for MaterialConsumptionEvent ref={}: {}",
                    event.getConsumptionRef(), ex.getMessage(), ex);
        }
    }
}
