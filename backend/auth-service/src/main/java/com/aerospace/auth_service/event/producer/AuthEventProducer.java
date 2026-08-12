package com.aerospace.auth_service.event.producer;

import com.aerospace.auth_service.event.KafkaTopics;
import com.aerospace.auth_service.event.PasswordResetRequestedEvent;
import com.aerospace.auth_service.event.SupplierInviteCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSupplierInviteCreated(SupplierInviteCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.SUPPLIER_INVITE_CREATED, event);
        log.info("[AUTH] Published SUPPLIER_INVITE_CREATED for supplierId={}", event.getSupplierId());
    }

    public void sendPasswordResetRequested(PasswordResetRequestedEvent event) {
        kafkaTemplate.send(KafkaTopics.PASSWORD_RESET_REQUESTED, event);
        log.info("[AUTH] Published PASSWORD_RESET_REQUESTED for email={}", event.getEmail());
    }
}
