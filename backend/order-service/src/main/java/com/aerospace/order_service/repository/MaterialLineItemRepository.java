package com.aerospace.order_service.repository;

import com.aerospace.order_service.entity.MaterialLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialLineItemRepository extends JpaRepository<MaterialLineItem, Long> {
    List<MaterialLineItem> findByWorkOrderId(Long workOrderId);
    List<MaterialLineItem> findByPartId(Long partId);
}