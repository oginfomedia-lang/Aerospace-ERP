package com.aerospace.auth_service.controller;

import com.aerospace.auth_service.dto.SupplierAuthDTOs.SupplierActivateRequest;
import com.aerospace.auth_service.dto.SupplierAuthDTOs.SupplierInviteRequest;
import com.aerospace.auth_service.dto.SupplierAuthDTOs.SupplierResendInviteRequest;
import com.aerospace.auth_service.service.SupplierAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/auth/suppliers")
@RequiredArgsConstructor
public class SupplierAuthController {
    private final SupplierAuthService supplierAuthService;

    //only staff who manage suppliers can invite one to the portal
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER', 'ADMIN')")
    @PostMapping("/invite")
    public ResponseEntity<Void> invite(@Valid @RequestBody SupplierInviteRequest request) {
        supplierAuthService.inviteSupplier(request);
        return ResponseEntity.accepted().build();
    }

    //public - the supplier isn't logged in yet, this is how they get a password
    @PostMapping("/activate")
    public ResponseEntity<Void> activate(@Valid @RequestBody SupplierActivateRequest request) {
        supplierAuthService.activate(request);
        return ResponseEntity.ok().build();
    }

    //re-sends a fresh activation link for a supplier whose invite email never arrived or expired
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER', 'ADMIN')")
    @PostMapping("/resend-invite")
    public ResponseEntity<Void> resendInvite(@Valid @RequestBody SupplierResendInviteRequest request) {
        supplierAuthService.resendInvite(request);
        return ResponseEntity.accepted().build();
    }

    //lets the Suppliers page show a per-supplier portal-account badge (NONE/PENDING/ACTIVE)
    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER', 'ADMIN')")
    @GetMapping("/invite-status")
    public ResponseEntity<Map<Long, String>> inviteStatus(@RequestParam List<Long> supplierIds) {
        return ResponseEntity.ok(supplierAuthService.getInviteStatuses(supplierIds));
    }

}