package com.aerospace.finance_service.event;

import com.aerospace.finance_service.entity.SupplierReference;
import com.aerospace.finance_service.repository.SupplierReferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Keeps finance-service's local SupplierReference mirror in sync with
 * procurement-service's Supplier master. This is a read-only cache — finance-service
 * never creates, updates, or deletes a real supplier, only reflects what procurement
 * publishes so journal lines / supplier payments can hold a real foreign key instead
 * of a free-text reference.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupplierEventConsumer {

    private final SupplierReferenceRepository supplierReferenceRepository;

    @KafkaListener(topics = "supplier-registered-events", groupId = "finance-service")
    public void onSupplierRegistered(SupplierRegisteredEvent event) {
        log.info("Received SupplierRegisteredEvent for supplier {} ({})",
                event.getSupplierName(), event.getVendorCode());
        try {
            // save() with a manually-assigned (non-generated) @Id is an idempotent
            // upsert-by-PK under Hibernate — safe if Kafka redelivers this message.
            SupplierReference ref = SupplierReference.builder()
                    .id(event.getSupplierId())
                    .supplierName(event.getSupplierName())
                    .vendorCode(event.getVendorCode())
                    .registeredBy(event.getRegisteredBy())
                    .build();
            supplierReferenceRepository.save(ref);
        } catch (Exception ex) {
            log.error("Failed to sync SupplierReference id={}: {}", event.getSupplierId(), ex.getMessage(), ex);
        }
    }
}
