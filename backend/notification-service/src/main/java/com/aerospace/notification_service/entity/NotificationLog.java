package com.aerospace.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Every notification sent is stored here — full audit trail
@Entity
@Table(name = "notification_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // What triggered this notification
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // Natural business key of the triggering event (PO/WO/FAI/CAPA/NCR number, part
    // id, etc.), when the upstream event carries one — used to de-dupe side effects
    // (websocket/email/log) on Kafka redelivery of the same message. Nullable: not
    // every event type carries a stable identity (see call sites in
    // NotificationServiceImpl for events where no natural key exists).
    private String eventId;

    // Who/what it's about
    private Long partId;
    private String partName;
    private String batchNumber;

    // The message content
    @Column(nullable = false, length = 1000)
    private String message;

    // Delivery status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    //Channel tracking
    private boolean websocketSent;
    private boolean emailSent;

    private String recipient;
    private String errorMessage;

    // Metadata from the triggering event
    private String inspectorName;
    private String location;
    private String severity;

    // Who performed the action, when the triggering event records one — null for
    // system/threshold-triggered notifications (e.g. LOW_STOCK, CALIBRATION_DUE).
    // Used to skip notifying the person who just did the thing themselves.
    private String actorUsername;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}