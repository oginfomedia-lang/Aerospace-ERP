package com.aerospace.procurement_service.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.aerospace.procurement_service.event.KafkaTopics;
import com.aerospace.procurement_service.event.MaterialShortageEvent;
import com.aerospace.procurement_service.event.QAStatusChangedEvent;
import com.aerospace.procurement_service.event.StockLowEvent;
import com.aerospace.procurement_service.event.VendorReturnEvent;
import com.aerospace.procurement_service.service.DebitNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {
    private final com.aerospace.procurement_service.service.PurchaseOrderService poService;
    private final DebitNoteService debitNoteService;

    // Topic string previously didn't match what inventory-service actually publishes
    // to ("inventory-stock-low" vs the real "stock-low-events"), so this listener
    // never fired. Fixed to use the shared constant.
    @KafkaListener(topics = KafkaTopics.STOCK_LOW, groupId = "procurement-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleStockLowEvent(StockLowEvent event) {
        //1.log it so we can see it working in the terminal
        log.warn("🚨 [PROCUREMENT ALERT] Low stock detected for Part: {} (ID: {}). Current Qty: {} (Reorder Level: {})",
            event.getPartName(),
            event.getPartId(),
            event.getCurrentQuantity(),
            event.getReorderLevel()
        );

        //2. Automatically create draft PO
        try {
            poService.createDraftPOFromStockLow(event);
        } catch (Exception e) {
            log.error("Failed to automatically create draft PO from stock low event", e);
        }
    }

    // WO-creation-time shortage → auto-raise a DRAFT PO for the shortfall.
    @KafkaListener(
        topics = KafkaTopics.MATERIAL_SHORTAGE,
        groupId = "procurement-service-group",
        containerFactory = "materialShortageKafkaListenerContainerFactory"
    )
    public void handleMaterialShortageEvent(@Payload MaterialShortageEvent event) {
        log.warn("🚨 [PROCUREMENT ALERT] Material shortage for WO {} — part {} short by {} units",
                event.getWoNumber(), event.getSkuCode(), event.getShortfallQty());
        try {
            poService.createDraftPOFromShortage(event);
        } catch (Exception e) {
            log.error("Failed to automatically create draft PO from shortage event", e);
        }
    }

    // QA approved/rejected a received batch — reflect it on the originating PO line
    // item, if the batch is traceable to one (event.poNumber != null).
    @KafkaListener(
        topics = KafkaTopics.QA_STATUS_CHANGED,
        groupId = "procurement-service-group",
        containerFactory = "qaStatusChangedKafkaListenerContainerFactory"
    )
    public void handleQaStatusChanged(@Payload QAStatusChangedEvent event) {
        try {
            poService.applyQaOutcome(event);
        } catch (Exception e) {
            log.error("Failed to apply QA outcome to purchase order", e);
        }
    }

    //rejected batch returned to vendor - auto raise a debit note against the supplier
    @KafkaListener(
        topics = KafkaTopics.VENDOR_RETURN,
        groupId = "procurement-service-group",
        containerFactory = "vendorReturnKafkaListenerContainerFactory"
    )
    public void handleVendorReturn(@Payload VendorReturnEvent event) {
        try {
            debitNoteService.createFromVendorReturn(event);
        } catch (Exception e) {
            log.error("Failed to create debit note from vendor return event", e);
        }
    }
}
