package com.aerospace.auth_service.service;

import com.aerospace.auth_service.dto.SupplierAuthDTOs.SupplierActivateRequest;
import com.aerospace.auth_service.dto.SupplierAuthDTOs.SupplierInviteRequest;
import com.aerospace.auth_service.dto.SupplierAuthDTOs.SupplierResendInviteRequest;
import com.aerospace.auth_service.entity.Role;
import com.aerospace.auth_service.entity.SupplierInvite;
import com.aerospace.auth_service.entity.User;
import com.aerospace.auth_service.event.SupplierInviteCreatedEvent;
import com.aerospace.auth_service.event.producer.AuthEventProducer;
import com.aerospace.auth_service.repository.RoleRepository;
import com.aerospace.auth_service.repository.SupplierInviteRepository;
import com.aerospace.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SupplierInviteRepository inviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEventProducer eventProducer;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final jakarta.servlet.http.HttpServletRequest httpRequest;

    @Value("${app.frontend.supplier-portal-base-url}")
    private String supplierPortalBaseUrl;

    @Value("${services.procurement.base-url}")
    private String procurementBaseUrl;

    @Transactional
    public void inviteSupplier(SupplierInviteRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("A login already exists for this email");
        }

        assertSupplierConfirmed(request.getSupplierId());

        Role supplierRole = roleRepository.findByName("SUPPLIER")
                .orElseThrow(() -> new IllegalStateException("SUPPLIER role not seeded"));

        Set<Role> roles = new HashSet<>();
        roles.add(supplierRole);

        // Placeholder password — unusable until activation sets a real one via /activate.
        User user = User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .supplierId(request.getSupplierId())
                .roles(roles)
                .enabled(false)
                .build();
        User saved = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        inviteRepository.save(SupplierInvite.builder()
                .token(token)
                .userId(saved.getId())
                .supplierId(request.getSupplierId())
                .email(request.getEmail())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .build());

        eventProducer.sendSupplierInviteCreated(SupplierInviteCreatedEvent.builder()
                .supplierId(request.getSupplierId())
                .supplierName(request.getSupplierName())
                .email(request.getEmail())
                .activationToken(token)
                .activationUrl(supplierPortalBaseUrl + "/supplier/activate?token=" + token)
                .build());
    }

    @Transactional
    public void activate(SupplierActivateRequest request) {
        SupplierInvite invite = inviteRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation link"));
        if (invite.isUsed()) {
            throw new IllegalStateException("This activation link has already been used");
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This activation link has expired — ask Procurement to re-invite you");
        }

        User user = userRepository.findById(invite.getUserId())
                .orElseThrow(() -> new IllegalStateException("Invite user no longer exists"));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        userRepository.save(user);

        invite.setUsed(true);
        inviteRepository.save(invite);
    }

    @Transactional
    public void resendInvite(SupplierResendInviteRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("No invite found for this email — use \"Invite to Portal\" instead"));
        if (user.isEnabled()) {
            throw new IllegalStateException("This supplier has already activated their account");
        }

        String token = UUID.randomUUID().toString();
        inviteRepository.save(SupplierInvite.builder()
                .token(token)
                .userId(user.getId())
                .supplierId(user.getSupplierId())
                .email(user.getEmail())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .build());

        eventProducer.sendSupplierInviteCreated(SupplierInviteCreatedEvent.builder()
                .supplierId(user.getSupplierId())
                .supplierName(request.getSupplierName())
                .email(user.getEmail())
                .activationToken(token)
                .activationUrl(supplierPortalBaseUrl + "/supplier/activate?token=" + token)
                .build());
    }

    // Lets the Suppliers page show "Not Invited" / "Pending Activation" / "Active" per supplier
    // without exposing full user records.
    @Transactional(readOnly = true)
    public Map<Long, String> getInviteStatuses(List<Long> supplierIds) {
        Map<Long, String> result = new HashMap<>();
        for (User user : userRepository.findBySupplierIdIn(supplierIds)) {
            result.put(user.getSupplierId(), user.isEnabled() ? "ACTIVE" : "PENDING");
        }
        return result;
    }

    // procurement-service is the source of truth for the confirmation decision, so
    // this is a live check (not a cached flag) — same pattern WorkOrderServiceImpl
    // already uses to validate against inventory-service before letting an action
    // through, with the caller's own JWT forwarded so the downstream call is authorized.
    private void assertSupplierConfirmed(Long supplierId) {
        String url = procurementBaseUrl + "/suppliers/" + supplierId;

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        String authHeader = httpRequest.getHeader(org.springframework.http.HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
            headers.set(org.springframework.http.HttpHeaders.AUTHORIZATION, authHeader);
        }

        com.aerospace.auth_service.dto.SupplierLookupResponse supplier;
        try {
            var response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(headers),
                    com.aerospace.auth_service.dto.SupplierLookupResponse.class);
            supplier = response.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            throw new IllegalStateException("Could not verify supplier confirmation status: " + ex.getStatusCode());
        }

        if (supplier == null || !"CONFIRMED".equals(supplier.getConfirmationStatus())) {
            throw new IllegalStateException(
                    "Supplier must be confirmed by QA/authority before being invited to the portal (current status: "
                            + (supplier != null ? supplier.getConfirmationStatus() : "UNKNOWN") + ")");
        }
    }
}
