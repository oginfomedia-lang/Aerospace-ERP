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
public class QualityCostEventConsumer {

    private final PostingEngine postingEngine;

    @KafkaListener(topics = "quality-cost-events", groupId = "finance-service")
    public void onQualityCost(QualityCostEvent event) {
        if (event.getTotalCost() == null) {
            log.warn("QualityCostEvent for {} has no cost — skipping", event.getQualityOrderRef());
            return;
        }
        log.info("Received QualityCostEvent for order {}", event.getQualityOrderRef());
        try {
            postingEngine.postFromEvent(PostingTriggerType.QUALITY_COST,
                    event.getTotalCost(), event.getCurrency(), "QUALITY_COST",
                    event.getQualityOrderRef(), event.getCostCenterId(), null);
        } catch (Exception ex) {
            log.error("Failed to post journal for QualityCostEvent ref={}: {}",
                    event.getQualityOrderRef(), ex.getMessage(), ex);
        }
    }
}
