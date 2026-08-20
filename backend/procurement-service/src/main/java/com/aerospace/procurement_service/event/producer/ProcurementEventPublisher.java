package com.aerospace.procurement_service.event.producer;

import com.aerospace.procurement_service.event.KafkaTopics;
import com.aerospace.procurement_service.event.PoApprovalEvent;
import com.aerospace.procurement_service.event.PrStatusEvent;
import com.aerospace.procurement_service.event.PurchaseOrderCreatedEvent;
import com.aerospace.procurement_service.event.QuotationAwardedEvent;
import com.aerospace.procurement_service.event.QuotationRejectedEvent;
import com.aerospace.procurement_service.event.RfqCreatedEvent;
import com.aerospace.procurement_service.event.VendorInvoiceApprovedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.aerospace.procurement_service.event.PoDeliveryOverdueEvent;
import com.aerospace.procurement_service.event.SupplierRegisteredEvent;
import com.aerospace.procurement_service.event.SupplierConfirmationDecidedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcurementEventPublisher {

    // KafkaTemplate is Spring's built-in tool for sending messages
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Define the topic name as a constant to avoid typos
    private static final String PO_CREATED_TOPIC = "procurement-po-created";

    public void publishPOCreatedEvent(PurchaseOrderCreatedEvent event) {
        try {
            // 1. Convert our Java Object into a JSON String
            String message = objectMapper.writeValueAsString(event);

            // 2. Send the message to the Kafka topic
            kafkaTemplate.send(PO_CREATED_TOPIC, message);

            log.info("📢 [PROCUREMENT EVENT] Published PO Created event for PO: {}", event.getPoNumber());

        } catch (JsonProcessingException e) {
            log.error("Failed to convert PurchaseOrderCreatedEvent to JSON", e);
        }
    }

    public void publishRfqCreated(RfqCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.RFQ_CREATED, event);
        log.info("📢 [PROCUREMENT EVENT] Published RFQ_CREATED — rfq={} supplier={}",
                event.getRfqNumber(), event.getSupplierName());
    }

    public void publishQuotationAwarded(QuotationAwardedEvent event) {
        kafkaTemplate.send(KafkaTopics.QUOTATION_AWARDED, event);
        log.info("📢 [PROCUREMENT EVENT] Published QUOTATION_AWARDED — rfq={} supplier={}",
                event.getRfqNumber(), event.getSupplierName());
    }

    public void publishQuotationRejected(QuotationRejectedEvent event) {
        kafkaTemplate.send(KafkaTopics.QUOTATION_REJECTED, event);
        log.info("📢 [PROCUREMENT EVENT] Published QUOTATION_REJECTED — rfq={} supplier={}",
                event.getRfqNumber(), event.getSupplierName());
    }

    public void publishPrStatusChanged(PrStatusEvent event) {
        kafkaTemplate.send(KafkaTopics.PR_STATUS_CHANGED, event);
        log.info("📢 [PROCUREMENT EVENT] Published PR_STATUS_CHANGED — pr={} status={}",
                event.getPrNumber(), event.getStatus());
    }

    public void publishPoApprovalStatusChanged(PoApprovalEvent event) {
        kafkaTemplate.send(KafkaTopics.PO_APPROVAL_STATUS, event);
        log.info("📢 [PROCUREMENT EVENT] Published PO_APPROVAL_STATUS — po={} status={}",
                event.getPoNumber(), event.getStatus());
    }

    public void publishPoDeliveryOverdue(PoDeliveryOverdueEvent event) {
        kafkaTemplate.send(KafkaTopics.PO_DELIVERY_OVERDUE, event);
        log.info("📢 [PROCUREMENT EVENT] Published PO_DELIVERY_OVERDUE — po={} supplier={} overdueHours={}",
                event.getPoNumber(), event.getSupplierName(), event.getHoursOverdue());
    }

    public void publishSupplierRegistered(SupplierRegisteredEvent event) {
        kafkaTemplate.send(KafkaTopics.SUPPLIER_REGISTERED, event);
        log.info("📢 [PROCUREMENT EVENT] Published SUPPLIER_REGISTERED — supplier={}", event.getSupplierName());
    }

    public void publishSupplierConfirmationDecided(SupplierConfirmationDecidedEvent event) {
        kafkaTemplate.send(KafkaTopics.SUPPLIER_CONFIRMATION_DECIDED, event);
        log.info("📢 [PROCUREMENT EVENT] Published SUPPLIER_CONFIRMATION_DECIDED — supplier={} status={}",
                event.getSupplierName(), event.getStatus());
    }

    public void publishVendorInvoiceApproved(VendorInvoiceApprovedEvent event) {
        kafkaTemplate.send(KafkaTopics.VENDOR_INVOICE_APPROVED, event);
        log.info("📢 [PROCUREMENT EVENT] Published VENDOR_INVOICE_APPROVED — invoice={} amount={} {}",
                event.getInvoiceNumber(), event.getTotalAmount(), event.getCurrency());
    }
}
