package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.CollectionActivity;
import com.aerospace.finance_service.entity.CollectionActivity.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionActivityRepository extends JpaRepository<CollectionActivity, Long> {
    List<CollectionActivity> findByCustomerRef(String customerRef);
    List<CollectionActivity> findByTenantId(String tenantId);
    List<CollectionActivity> findByCustomerRefAndStatus(String customerRef, ActivityStatus status);
}
