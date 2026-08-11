package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.Tooling;
import com.aerospace.production_resource_service.entity.ToolingStatus;
import com.aerospace.production_resource_service.entity.ToolingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolingRepository extends JpaRepository<Tooling, Long> {
    List<Tooling> findByStatus(ToolingStatus status);
    List<Tooling> findByToolingType(ToolingType toolingType);
}
