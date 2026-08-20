package com.aerospace.finance_service.event;

import com.aerospace.finance_service.entity.CustomerReference;
import com.aerospace.finance_service.repository.CustomerReferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Keeps finance-service's local CustomerReference mirror in sync with sales-service's
 * Customer master. Read-only cache, mirrors SupplierEventConsumer exactly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventConsumer {

    private final CustomerReferenceRepository customerReferenceRepository;

    @KafkaListener(topics = "customer-registered-events", groupId = "finance-service")
    public void onCustomerRegistered(CustomerRegisteredEvent event) {
        log.info("Received CustomerRegisteredEvent for customer {} ({})", event.getName(), event.getCustomerCode());
        try {
            CustomerReference ref = CustomerReference.builder()
                    .id(event.getCustomerId())
                    .customerName(event.getName())
                    .customerCode(event.getCustomerCode())
                    .status("ACTIVE")
                    .registeredBy(event.getRegisteredBy())
                    .build();
            customerReferenceRepository.save(ref);
        } catch (Exception ex) {
            log.error("Failed to sync CustomerReference id={}: {}", event.getCustomerId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = "customer-updated-events", groupId = "finance-service")
    public void onCustomerUpdated(CustomerUpdatedEvent event) {
        log.info("Received CustomerUpdatedEvent for customer {} ({}) status={}",
                event.getName(), event.getCustomerCode(), event.getStatus());
        try {
            CustomerReference existing = customerReferenceRepository.findById(event.getCustomerId()).orElse(null);
            CustomerReference ref = CustomerReference.builder()
                    .id(event.getCustomerId())
                    .customerName(event.getName())
                    .customerCode(event.getCustomerCode())
                    .status(event.getStatus())
                    .registeredBy(existing != null ? existing.getRegisteredBy() : "UNKNOWN")
                    .build();
            customerReferenceRepository.save(ref);
        } catch (Exception ex) {
            log.error("Failed to sync CustomerReference update id={}: {}", event.getCustomerId(), ex.getMessage(), ex);
        }
    }
}
