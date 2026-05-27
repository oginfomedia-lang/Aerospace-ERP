package com.aerospace.reporting_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aerospace.reporting_service.entity.ProductionFact;
import com.aerospace.reporting_service.event.ProductionOrderCompletedEvent;
import com.aerospace.reporting_service.event.ProductionOrderCreatedEvent;
import com.aerospace.reporting_service.repository.ProductionFactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionEventConsumer {

    private final ProductionFactRepository productionFactRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "production-order-created", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleProductionOrderCreated(String message) {
        try {
            ProductionOrderCreatedEvent event = objectMapper.readValue(message, ProductionOrderCreatedEvent.class);
            log.info("🏭 Production order created: {}", event.getPoNumber());
            ProductionFact fact = new ProductionFact();
            fact.setProductionOrderId(event.getProductionOrderId());
            fact.setPoNumber(event.getPoNumber());
            fact.setProductName(event.getProductName());
            fact.setProductCode(event.getProductCode());
            fact.setPlannedQuantity(event.getPlannedQuantity());
            fact.setPriority(event.getPriority());
            fact.setStatus("PLANNED");
            fact.setCreatedAt(event.getCreatedAt());
            productionFactRepository.save(fact);
        } catch (Exception e) {
            log.error("Failed to parse production-order-created message: {}", message, e);
        }
    }

    @KafkaListener(topics = "production-order-completed", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleProductionOrderCompleted(String message) {
        try {
            ProductionOrderCompletedEvent event = objectMapper.readValue(message, ProductionOrderCompletedEvent.class);
            log.info("✅ Production order completed: {}", event.getPoNumber());
            productionFactRepository.findById(event.getProductionOrderId()).ifPresent(fact -> {
                fact.setProducedQuantity(event.getProducedQuantity());
                fact.setRejectedQuantity(event.getRejectedQuantity());
                fact.setQualityPassed(event.getQualityPassed());
                fact.setStatus("COMPLETED");
                fact.setCompletedAt(event.getCompletedAt());
                productionFactRepository.save(fact);
            });
        } catch (Exception e) {
            log.error("Failed to parse production-order-completed message: {}", message, e);
        }
    }
}