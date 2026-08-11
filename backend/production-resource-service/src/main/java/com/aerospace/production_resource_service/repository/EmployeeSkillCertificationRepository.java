package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.EmployeeSkillCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeSkillCertificationRepository extends JpaRepository<EmployeeSkillCertification, Long> {
    List<EmployeeSkillCertification> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<EmployeeSkillCertification> findByEmployeeIdAndSkillIdOrderByCreatedAtDesc(Long employeeId, Long skillId);

    // Convenience filter for listEmployees?skillId= — not the hard eligibility gate
    // (that reads the latest-per-skill summary computed in EmployeeServiceImpl), just
    // narrows "has ever had an active, non-expired cert row for this skill."
    @Query("select distinct c.employee.id from EmployeeSkillCertification c " +
            "where c.skill.id = :skillId and c.status = com.aerospace.production_resource_service.entity.CertificationStatus.ACTIVE " +
            "and (c.expiryDate is null or c.expiryDate >= current_date)")
    List<Long> findActiveEmployeeIdsBySkill(Long skillId);
}
