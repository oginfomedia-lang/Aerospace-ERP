package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.MaterialIssueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialIssueRecordRepository extends JpaRepository<MaterialIssueRecord, Long> {
    List<MaterialIssueRecord> findByWorkOrderId(Long workOrderId);
    List<MaterialIssueRecord> findByLineItemId(Long lineItemId);
}
