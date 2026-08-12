package com.aerospace.auth_service.event;
import lombok.*;

// Published so notification-service (not auth) sends the reset email — keeps this
// repo's "no cross-service REST for fire-and-forget notifications" rule intact.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestedEvent {
    private String email;
    private String username;
    private String resetLink;
    private int expiryMinutes;
}
