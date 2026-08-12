package com.aerospace.auth_service.controller;

import com.aerospace.auth_service.dto.AdminCreateUserRequest;
import com.aerospace.auth_service.dto.UpdateUserRolesRequest;
import com.aerospace.auth_service.dto.UpdateUserStatusRequest;
import com.aerospace.auth_service.dto.UserResponse;
import com.aerospace.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUserWithRoles(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(authService.listAllUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(authService.setUserEnabled(id, request.getEnabled()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/roles")
    public ResponseEntity<UserResponse> updateRoles(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(authService.updateUserRoles(id, request.getRoles()));
    }
}