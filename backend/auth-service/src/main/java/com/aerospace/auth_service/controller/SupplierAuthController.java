package com.aerospace.auth_service.controller;

import com.aerospace.auth_service.dto.SupplierAuthDTOs.SupplierActivateRequest;
import com.aerospace.auth_service.dto.SupplierAuthDTOs.SupplierInviteRequest;
import com.aerospace.auth_service.service.SupplierAuthService;

import io.micrometer.core.ipc.http.HttpSender.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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
    
}