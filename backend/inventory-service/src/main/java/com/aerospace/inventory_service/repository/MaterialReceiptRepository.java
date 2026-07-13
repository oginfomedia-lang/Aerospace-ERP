package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.MaterialReceipt;
import com.aerospace.inventory_service.entity.MaterialReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialReceiptRepository extends JpaRepository<MaterialReceipt, Long> {
    List<MaterialReceipt> findByStatus(MaterialReceiptStatus status);
}
