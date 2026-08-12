package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordResetRequestedEvent {
    private String email;
    private String username;
    private String resetLink;
    private int expiryMinutes;
}
