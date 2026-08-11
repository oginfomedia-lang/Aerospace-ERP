package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.EmployeeLeave;
import com.aerospace.production_resource_service.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Long> {
    List<EmployeeLeave> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    // The occupyEmployee gate: is there an APPROVED leave row whose date range covers
    // today? startDate <= today AND endDate >= today.
    boolean existsByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId, LeaveStatus status, LocalDate today1, LocalDate today2);
}
