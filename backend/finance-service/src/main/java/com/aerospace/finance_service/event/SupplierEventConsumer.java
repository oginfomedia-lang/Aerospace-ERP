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

    @KafkaListener(topics = "supplier-updated-events", groupId = "finance-service")
    public void onSupplierUpdated(SupplierUpdatedEvent event) {
        log.info("Received SupplierUpdatedEvent for supplier {} ({}) active={}",
                event.getSupplierName(), event.getVendorCode(), event.isActive());
        try {
            // The update event doesn't carry registeredBy — preserve whatever the
            // original CREATE-time sync already wrote, since this is still an
            // upsert-by-PK on an existing row, not a fresh one.
            SupplierReference existing = supplierReferenceRepository.findById(event.getSupplierId()).orElse(null);
            SupplierReference ref = SupplierReference.builder()
                    .id(event.getSupplierId())
                    .supplierName(event.getSupplierName())
                    .vendorCode(event.getVendorCode())
                    .active(event.isActive())
                    .registeredBy(existing != null ? existing.getRegisteredBy() : "UNKNOWN")
                    .build();
            supplierReferenceRepository.save(ref);
        } catch (Exception ex) {
            log.error("Failed to sync SupplierReference update id={}: {}", event.getSupplierId(), ex.getMessage(), ex);
        }
    }
}
