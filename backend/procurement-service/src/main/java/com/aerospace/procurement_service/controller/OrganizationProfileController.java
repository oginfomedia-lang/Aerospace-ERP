package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.dto.OrganizationProfileRequest;
import com.aerospace.procurement_service.entity.OrganizationProfile;
import com.aerospace.procurement_service.service.OrganizationProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organization-profile")
@RequiredArgsConstructor
public class OrganizationProfileController {

    private final OrganizationProfileService organizationProfileService;

    @GetMapping
    public ResponseEntity<OrganizationProfile> getProfile() {
        return ResponseEntity.ok(organizationProfileService.getProfile());
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping
    public ResponseEntity<OrganizationProfile> updateProfile(@Valid @RequestBody OrganizationProfileRequest request) {
        return ResponseEntity.ok(organizationProfileService.updateProfile(request));
    }
}
