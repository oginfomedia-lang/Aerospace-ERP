package com.aerospace.notification_service.repository;

import com.aerospace.notification_service.entity.NotificationLog;
import com.aerospace.notification_service.entity.NotificationStatus;
import com.aerospace.notification_service.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationLog, Long> {

    // Last N notifications — controller uses top 20
    List<NotificationLog> findTop20ByOrderByCreatedAtDesc();

    // All notifications for a specific part
    List<NotificationLog> findByPartIdOrderByCreatedAtDesc(Long partId);

    // Filter by type
    List<NotificationLog> findByTypeOrderByCreatedAtDesc(NotificationType type);

    // Filter by status
    List<NotificationLog> findByStatusOrderByCreatedAtDesc(NotificationStatus status);

    // Filter by batch
    List<NotificationLog> findByBatchNumberOrderByCreatedAtDesc(String batchNumber);
}