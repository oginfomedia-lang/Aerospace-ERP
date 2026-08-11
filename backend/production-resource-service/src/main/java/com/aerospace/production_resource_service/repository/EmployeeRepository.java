package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.Employee;
import com.aerospace.production_resource_service.entity.EmployeeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByStatus(EmployeeStatus status);
    boolean existsByEmployeeCode(String employeeCode);

    // Same purpose as MachineRepository.findByIdForUpdate — see
    // EmployeeReservationServiceImpl for the full concurrency explanation.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);
}
