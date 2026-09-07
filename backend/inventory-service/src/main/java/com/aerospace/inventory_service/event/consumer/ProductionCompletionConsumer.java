package com.aerospace.inventory_service.event.consumer;

import com.aerospace.inventory_service.entity.Part;
import com.aerospace.inventory_service.entity.StockBalance;
import com.aerospace.inventory_service.entity.StockStatus;
import com.aerospace.inventory_service.event.KafkaTopics;
import com.aerospace.inventory_service.event.ProductionOrderCompletedEvent;
import com.aerospace.inventory_service.event.StockReceivedEvent;
import com.aerospace.inventory_service.event.producer.InventoryProducer;
import com.aerospace.inventory_service.repository.PartRepository;
import com.aerospace.inventory_service.repository.StockBalanceRepository;
import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.inventory_service.service.LocationService;
import com.aerospace.inventory_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// When a Production Order completes with an Output Part set, credit that quantity as new
// APPROVED stock — closes the manufacturing loop (finished goods become issuable/sellable
// stock same as a GRN receipt). Kafka-driven, consistent with WorkOrderShortageConsumer.
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductionCompletionConsumer {

    private final PartRepository partRepository;
    private final StockBalanceRepository balanceRepository;
    private final DocumentSequenceService documentSequenceService;
    private final TransactionService transactionService;
    private final LocationService locationService;
    private final InventoryProducer inventoryProducer;

    @KafkaListener(
        topics = KafkaTopics.PRODUCTION_ORDER_COMPLETED,
        containerFactory = "productionOrderCompletedKafkaListenerContainerFactory"
    )
    public void consumeProductionOrderCompleted(@Payload ProductionOrderCompletedEvent event) {
        if (event.getOutputPartId() == null) {
            log.warn("[PRODUCTION-CREDIT] PO {} completed with no outputPartId — skipping stock credit",
                    event.getPoNumber());
            return;
        }
        if (Boolean.FALSE.equals(event.getQualityPassed())) {
            log.warn("[PRODUCTION-CREDIT] PO {} failed QA — skipping stock credit", event.getPoNumber());
            return;
        }
        int qty = event.getProducedQuantity() != null ? event.getProducedQuantity() : 0;
        if (qty <= 0) {
            log.warn("[PRODUCTION-CREDIT] PO {} has no produced quantity — skipping stock credit",
                    event.getPoNumber());
            return;
        }
        if (event.getOutputLocationCode() == null || event.getOutputLocationCode().isBlank()) {
            log.warn("[PRODUCTION-CREDIT] PO {} completed with no outputLocationCode — skipping stock credit",
                    event.getPoNumber());
            return;
        }
        try {
            locationService.assertActive(event.getOutputLocationCode());
        } catch (Exception ex) {
            log.error("[PRODUCTION-CREDIT] PO {} references invalid/inactive location '{}' — skipping stock credit: {}",
                    event.getPoNumber(), event.getOutputLocationCode(), ex.getMessage());
            return;
        }

        Part part = partRepository.findById(event.getOutputPartId()).orElse(null);
        if (part == null) {
            log.warn("[PRODUCTION-CREDIT] PO {} references unknown outputPartId={} — skipping",
                    event.getPoNumber(), event.getOutputPartId());
            return;
        }

        // Dedup against Kafka at-least-once redelivery: this same PRODUCTION_ORDER_COMPLETED
        // event landing twice must not credit finished-goods stock twice.
        String sourceRef = event.getPoNumber() != null && !event.getPoNumber().isBlank()
                ? event.getPoNumber()
                : (event.getProductionOrderId() != null ? "PO_ID:" + event.getProductionOrderId() : null);
        if (sourceRef != null && balanceRepository.existsBySourceProductionOrderRef(sourceRef)) {
            log.warn("[PRODUCTION-CREDIT] PO {} already credited to stock — skipping duplicate delivery",
                    event.getPoNumber());
            return;
        }

        String batch = documentSequenceService.generate("BATCH");
        StockBalance balance = StockBalance.builder()
                .partId(part.getId())
                .batchNumber(batch)
                .quantity(qty)
                .status(StockStatus.APPROVED)
                .uom(part.getUom())
                .location(event.getOutputLocationCode())
                .sourceProductionOrderRef(sourceRef)
                .lastUpdated(LocalDateTime.now())
                .build();
        balanceRepository.save(balance);

        transactionService.logTransaction(part.getId(), batch, "PRODUCTION_RECEIPT", qty,
                event.getPoNumber(), "Credited from completed Production Order " + event.getPoNumber());

        log.info("[PRODUCTION-CREDIT] Credited {} units of {} (batch {}) from PO {}",
                qty, part.getSkuCode(), batch, event.getPoNumber());

        try {
            inventoryProducer.sendStockReceivedEvent(StockReceivedEvent.builder()
                    .partId(part.getId())
                    .partName(part.getName())
                    .batchNumber(batch)
                    .quantityReceived(qty)
                    .supplier("PRODUCTION: " + event.getPoNumber())
                    .location(event.getOutputLocationCode())
                    .receivedBy(event.getSupervisorName())
                    .build());
        } catch (Exception ex) {
            log.warn("[KAFKA] STOCK_RECEIVED publish failed for PO {}: {}", event.getPoNumber(), ex.getMessage());
        }
    }
}