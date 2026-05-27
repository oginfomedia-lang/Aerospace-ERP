package com.aerospace.reporting_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aerospace.reporting_service.entity.WorkOrderFact;
import com.aerospace.reporting_service.event.WorkOrderCompletedEvent;
import com.aerospace.reporting_service.event.WorkOrderCreatedEvent;
import com.aerospace.reporting_service.event.WorkOrderStatusChangedEvent;
import com.aerospace.reporting_service.repository.WorkOrderFactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkOrderEventConsumer {

    private final WorkOrderFactRepository workOrderFactRepository;
    private final ObjectMapper objectMapper;

    
    @KafkaListener(topics = "work-order-created-events", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleWorkOrderCreated(String message) {
        try {
            WorkOrderCreatedEvent event = objectMapper.readValue(message, WorkOrderCreatedEvent.class);
            log.info("🛠️ Work order created: {}", event.getWoNumber());
            WorkOrderFact fact = new WorkOrderFact();
            fact.setWorkOrderId(event.getWorkOrderId());
            fact.setWoNumber(event.getWoNumber());
            fact.setAircraftId(event.getAircraftId());
            fact.setTaskDescription(event.getTaskDescription());
            fact.setPriority(event.getPriority());
            fact.setStatus(event.getStatus());
            fact.setCreatedBy(event.getCreatedBy());
            fact.setLocation(event.getLocation());
            fact.setCreatedAt(event.getCreatedAt());
            workOrderFactRepository.save(fact);
        } catch (Exception e) {
            log.error("Failed to parse work-order-created message: {}", message, e);
        }
    }

    @KafkaListener(topics = "work-order-updated-events", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleWorkOrderStatusChanged(String message) {
        try {
            WorkOrderStatusChangedEvent event = objectMapper.readValue(message, WorkOrderStatusChangedEvent.class);
            log.info("🔄 Work order {} status: {} → {}", event.getWoNumber(), event.getFromStatus(), event.getToStatus());
            workOrderFactRepository.findById(event.getWorkOrderId()).ifPresent(fact -> {
                fact.setStatus(event.getToStatus());
                if ("IN_PROGRESS".equals(event.getToStatus())) {
                    fact.setStartedAt(event.getChangedAt());
                }
                if ("COMPLETED".equals(event.getToStatus())) {
                    fact.setCompletedAt(event.getChangedAt());
                    if (fact.getStartedAt() != null) {
                        long days = Duration.between(fact.getStartedAt(), fact.getCompletedAt()).toDays();
                        fact.setDaysToComplete((int) days);
                    }
                }
                workOrderFactRepository.save(fact);
            });
        } catch (Exception e) {
            log.error("Failed to parse work-order-updated message: {}", message, e);
        }
    }

    @KafkaListener(topics = "work-order-completed-events", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handleWorkOrderCompleted(String message) {
        try {
            WorkOrderCompletedEvent event = objectMapper.readValue(message, WorkOrderCompletedEvent.class);
            log.info("✅ Work order completed: {}", event.getWoNumber());
            workOrderFactRepository.findById(event.getWorkOrderId()).ifPresent(fact -> {
                fact.setStatus("COMPLETED");
                fact.setCompletedAt(event.getCompletedAt());
                workOrderFactRepository.save(fact);
            });
        } catch (Exception e) {
            log.error("Failed to parse work-order-completed message: {}", message, e);
        }
    }
}