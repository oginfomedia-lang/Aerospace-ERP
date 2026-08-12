package com.aerospace.production_service.repository;

import com.aerospace.production_service.entity.RoutingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutingTemplateRepository extends JpaRepository<RoutingTemplate, Long> {
    List<RoutingTemplate> findByPartNumber(String partNumber);
    Optional<RoutingTemplate> findFirstByPartNumberAndIsActiveTrueAndIsCurrentRevisionTrue(String partNumber);
    List<RoutingTemplate> findByPartNumberOrderByRevisionNumberDesc(String partNumber);
}
