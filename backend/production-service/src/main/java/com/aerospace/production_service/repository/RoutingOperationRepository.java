package com.aerospace.production_service.repository;

import com.aerospace.production_service.entity.RoutingOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutingOperationRepository extends JpaRepository<RoutingOperation, Long> {
}
