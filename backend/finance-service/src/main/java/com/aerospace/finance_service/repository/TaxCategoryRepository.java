package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.TaxCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxCategoryRepository extends JpaRepository<TaxCategory, Long> {
}
