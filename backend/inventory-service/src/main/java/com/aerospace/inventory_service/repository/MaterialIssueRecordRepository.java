package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.MaterialIssueRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialIssueRecordRepository extends JpaRepository<MaterialIssueRecord, Long> {
    List<MaterialIssueRecord> findByWorkOrderId(Long workOrderId);
    List<MaterialIssueRecord> findByLineItemId(Long lineItemId);
    List<MaterialIssueRecord> findByPartId(Long partId);
}
