package com.aerospace.auth_service.repository;

import com.aerospace.auth_service.entity.SupplierInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SupplierInviteRepository extends JpaRepository<SupplierInvite, Long> {
    Optional<SupplierInvite> findByToken(String token);
}