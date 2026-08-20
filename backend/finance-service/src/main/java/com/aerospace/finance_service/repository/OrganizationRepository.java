package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByOrgCode(String orgCode);
    List<Organization> findByActiveStatusTrue();
    boolean existsByOrgCode(String orgCode);
}
