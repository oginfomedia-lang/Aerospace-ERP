package com.aerospace.auth_service.service;

import com.aerospace.auth_service.entity.User;
import com.aerospace.auth_service.event.PasswordResetRequestedEvent;
import com.aerospace.auth_service.event.producer.AuthEventProducer;
import com.aerospace.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEventProducer authEventProducer;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    private static final int EXPIRY_MINUTES = 30;

    @Transactional
    public void requestReset(String email) {
        // Always behave the same whether or not the email exists — don't leak
        // which emails are registered via response-timing/shape differences.
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
            userRepository.save(user);

            String resetLink = frontendBaseUrl + "/reset-password?token=" + token;
            authEventProducer.sendPasswordResetRequested(PasswordResetRequestedEvent.builder()
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .resetLink(resetLink)
                    .expiryMinutes(EXPIRY_MINUTES)
                    .build());
            log.info("Password reset requested for user: {}", user.getUsername());
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset completed for user: {}", user.getUsername());
    }
}
