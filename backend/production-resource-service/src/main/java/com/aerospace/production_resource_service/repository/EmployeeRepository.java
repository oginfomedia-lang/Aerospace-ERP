package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.Employee;
import com.aerospace.production_resource_service.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByStatus(EmployeeStatus status);
    List<Employee> findBySkillsId(Long skillId);
    List<Employee> findByStatusAndSkillsId(EmployeeStatus status, Long skillId);
    boolean existsByEmployeeCode(String employeeCode);
}
