package com.aerospace.notification_service.entity;

public enum NotificationStatus {
    PENDING,    // created but not yet dispatched
    SENT,       // successfully delivered (WebSocket + Email)
    FAILED      // dispatch failed — see errorMessage on NotificationLog
}
