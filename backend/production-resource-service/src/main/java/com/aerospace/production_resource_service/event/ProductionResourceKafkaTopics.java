package com.aerospace.production_resource_service.event;

public class ProductionResourceKafkaTopics {
    // Published by MaintenanceDueScheduler, consumed by notification-service
    public static final String MAINTENANCE_DUE = "production-resource.maintenance.due";
}
