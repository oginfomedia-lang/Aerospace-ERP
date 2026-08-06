package com.aerospace.reporting_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aerospace.reporting_service.entity.ProcurementFact;
import com.aerospace.reporting_service.event.PurchaseOrderCreatedEvent;
import com.aerospace.reporting_service.repository.ProcurementFactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcurementEventConsumer {

    private final ProcurementFactRepository procurementFactRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "procurement-po-created", containerFactory = "reportingKafkaListenerContainerFactory")
    public void handlePurchaseOrderCreated(String message) {
        try {
            PurchaseOrderCreatedEvent event = objectMapper.readValue(message, PurchaseOrderCreatedEvent.class);
            log.info("📄 Purchase order created: {} from {}", event.getPoNumber(), event.getSupplierName());
            ProcurementFact fact = new ProcurementFact();
            fact.setPurchaseOrderId(event.getPurchaseOrderId());
            fact.setPoNumber(event.getPoNumber());
            fact.setSupplierName(event.getSupplierName());
            fact.setTotalAmount(event.getTotalAmount());
            fact.setCurrency(event.getCurrency());
            fact.setOrderDate(LocalDateTime.now());
            fact.setDeliveryDate(event.getExpectedDeliveryDate());
            fact.setStatus("SENT_TO_VENDOR");
            procurementFactRepository.save(fact);
        } catch (Exception e) {
            log.error("Failed to parse purchase-order-created message: {}", message, e);
        }
    }
}