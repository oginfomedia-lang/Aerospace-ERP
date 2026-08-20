package com.aerospace.sales_service.event.producer;

import com.aerospace.sales_service.event.KafkaTopics;
import com.aerospace.sales_service.event.SalesOrderDispatchedEvent;
import com.aerospace.sales_service.event.CustomerRegisteredEvent;
import com.aerospace.sales_service.event.CustomerUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSalesOrderDispatched(Long orderId, String orderNumber, String dispatchNumber,
                                          String trackingNumber, Long customerId, String dispatchedBy,
                                          BigDecimal totalAmount, String currency) {
        SalesOrderDispatchedEvent event = SalesOrderDispatchedEvent.builder()
                .salesOrderId(orderId).orderNumber(orderNumber).dispatchNumber(dispatchNumber)
                .trackingNumber(trackingNumber).customerId(customerId).dispatchedBy(dispatchedBy)
                .totalAmount(totalAmount).currency(currency).build();
        log.info("[KAFKA] Publishing SALES_ORDER_DISPATCHED → orderNumber={}", orderNumber);
        kafkaTemplate.send(KafkaTopics.SALES_ORDER_DISPATCHED, String.valueOf(orderId), event);
    }

    public void sendCustomerRegistered(Long customerId, String customerCode, String name) {
        CustomerRegisteredEvent event = CustomerRegisteredEvent.builder()
                .customerId(customerId).customerCode(customerCode).name(name)
                .registeredBy("SYSTEM").build();
        log.info("[KAFKA] Publishing CUSTOMER_REGISTERED → customerCode={}", customerCode);
        kafkaTemplate.send(KafkaTopics.CUSTOMER_REGISTERED, String.valueOf(customerId), event);
    }

    public void sendCustomerUpdated(Long customerId, String customerCode, String name, String status) {
        CustomerUpdatedEvent event = CustomerUpdatedEvent.builder()
                .customerId(customerId).customerCode(customerCode).name(name)
                .status(status).updatedBy("SYSTEM").build();
        log.info("[KAFKA] Publishing CUSTOMER_UPDATED → customerCode={} status={}", customerCode, status);
        kafkaTemplate.send(KafkaTopics.CUSTOMER_UPDATED, String.valueOf(customerId), event);
    }
}
