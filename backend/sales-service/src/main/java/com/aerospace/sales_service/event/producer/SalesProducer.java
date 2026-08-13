package com.aerospace.sales_service.event.producer;

import com.aerospace.sales_service.event.KafkaTopics;
import com.aerospace.sales_service.event.SalesOrderDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSalesOrderDispatched(Long orderId, String orderNumber, String dispatchNumber,
                                          String trackingNumber, Long customerId, String dispatchedBy) {
        SalesOrderDispatchedEvent event = SalesOrderDispatchedEvent.builder()
                .salesOrderId(orderId).orderNumber(orderNumber).dispatchNumber(dispatchNumber)
                .trackingNumber(trackingNumber).customerId(customerId).dispatchedBy(dispatchedBy).build();
        log.info("[KAFKA] Publishing SALES_ORDER_DISPATCHED → orderNumber={}", orderNumber);
        kafkaTemplate.send(KafkaTopics.SALES_ORDER_DISPATCHED, String.valueOf(orderId), event);
    }
}
