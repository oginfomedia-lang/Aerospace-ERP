package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.TaxJurisdiction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxJurisdictionRepository extends JpaRepository<TaxJurisdiction, Long> {
}
