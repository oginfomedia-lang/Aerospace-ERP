package com.aerospace.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime expiryDate;

    // When the ORIGINAL login happened — carried forward unchanged through every
    // rotation of this refresh-token chain (unlike expiryDate, which resets on every
    // refresh). Lets AuthService enforce an absolute session lifetime on top of the
    // per-token expiry, so an active user can't stay logged in forever via rolling
    // refreshes.
    private LocalDateTime sessionStartAt;

    @Builder.Default
    private boolean revoked = false;

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }
}
