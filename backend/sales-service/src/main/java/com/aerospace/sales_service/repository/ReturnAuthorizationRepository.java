package com.aerospace.sales_service.repository;

import com.aerospace.sales_service.entity.ReturnAuthorization;
import com.aerospace.sales_service.entity.ReturnDisposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnAuthorizationRepository extends JpaRepository<ReturnAuthorization, Long> {
    List<ReturnAuthorization> findBySalesOrder_Id(Long salesOrderId);
    List<ReturnAuthorization> findByDisposition(ReturnDisposition disposition);
}
