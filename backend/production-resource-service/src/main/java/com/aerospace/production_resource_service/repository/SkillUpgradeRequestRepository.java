package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.SkillUpgradeRequest;
import com.aerospace.production_resource_service.entity.SkillUpgradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillUpgradeRequestRepository extends JpaRepository<SkillUpgradeRequest, Long> {
    List<SkillUpgradeRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<SkillUpgradeRequest> findByStatusOrderByCreatedAtAsc(SkillUpgradeStatus status);
}
