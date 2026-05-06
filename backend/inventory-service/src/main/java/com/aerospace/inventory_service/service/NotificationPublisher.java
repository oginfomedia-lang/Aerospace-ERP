package com.aerospace.inventory_service.service;

import com.aerospace.inventory_service.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStockReceived(Long partId, String partName, String batchNumber,
                                      int qty, String supplier, String location,
                                      String receivedBy) {
        var event = Map.of(
            "partId",             partId,
            "partName",           partName,
            "batchNumber",        batchNumber,
            "quantityReceived",   qty,
            "supplier",           supplier != null ? supplier : "Unknown",
            "location",           location != null ? location : "",
            "receivedBy",         receivedBy != null ? receivedBy : "System"
        );
        kafkaTemplate.send(KafkaTopics.STOCK_RECEIVED, String.valueOf(partId), event);
        log.info("[KAFKA] Published STOCK_RECEIVED → partId={}", partId);
    }

    public void publishPartCreated(Long partId, String partName, String batchNumber,
                                    int initialQty, int reorderLevel,
                                    String location, String createdBy) {
        var event = Map.of(
            "partId",          partId,
            "partName",        partName,
            "batchNumber",     batchNumber != null ? batchNumber : "",
            "initialQuantity", initialQty,
            "reorderLevel",    reorderLevel,
            "location",        location != null ? location : "",
            "createdBy",       createdBy != null ? createdBy : "System"
        );
        kafkaTemplate.send(KafkaTopics.PART_CREATED, String.valueOf(partId), event);
        log.info("[KAFKA] Published PART_CREATED → partId={}", partId);
    }

    public void publishMaterialIssued(Long partId, String partName, String batchNumber,
                                       int qtyIssued, int remainingQty,
                                       String issuedTo, String issuedBy, String location) {
        var event = Map.of(
            "partId",            partId,
            "partName",          partName,
            "batchNumber",       batchNumber,
            "quantityIssued",    qtyIssued,
            "remainingQuantity", remainingQty,
            "issuedTo",          issuedTo != null ? issuedTo : "Production",
            "issuedBy",          issuedBy != null ? issuedBy : "System",
            "location",          location != null ? location : ""
        );
        kafkaTemplate.send(KafkaTopics.MATERIAL_ISSUED, String.valueOf(partId), event);
        log.info("[KAFKA] Published MATERIAL_ISSUED → partId={}", partId);
    }

    public void publishStockLow(Long partId, String partName, String batchNumber,
                                 int currentQty, int reorderLevel, String location) {
        var event = Map.of(
            "partId",           partId,
            "partName",         partName,
            "batchNumber",      batchNumber != null ? batchNumber : "",
            "currentQuantity",  currentQty,
            "reorderLevel",     reorderLevel,
            "location",         location != null ? location : ""
        );
        kafkaTemplate.send(KafkaTopics.STOCK_LOW, String.valueOf(partId), event);
        log.info("[KAFKA] Published STOCK_LOW → partId={}", partId);
    }

    public void publishQaStatusChanged(Long partId, String partName, String batchNumber,
                                        String newStatus, String inspectorName,
                                        String location, String remarks,
                                        boolean batchExpired, String expiryDate) {
        var event = Map.of(
            "partId",        partId,
            "partName",      partName,
            "batchNumber",   batchNumber,
            "newStatus",     newStatus,
            "decision",      newStatus,
            "inspectorName", inspectorName != null ? inspectorName : "",
            "location",      location != null ? location : "",
            "remarks",       remarks != null ? remarks : "",
            "batchExpired",  batchExpired,
            "expiryDate",    expiryDate != null ? expiryDate : ""
        );
        kafkaTemplate.send(KafkaTopics.QA_STATUS_CHANGED, String.valueOf(partId), event);
        log.info("[KAFKA] Published QA_STATUS_CHANGED → partId={} status={}", partId, newStatus);
    }
}