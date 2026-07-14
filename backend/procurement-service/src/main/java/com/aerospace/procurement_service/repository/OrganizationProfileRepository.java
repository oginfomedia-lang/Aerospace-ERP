package com.aerospace.procurement_service.repository;

import com.aerospace.procurement_service.entity.OrganizationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationProfileRepository extends JpaRepository<OrganizationProfile, Long> {
}
