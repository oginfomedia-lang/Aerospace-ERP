package com.aerospace.auth_service.service;

import com.aerospace.auth_service.dto.*;
import com.aerospace.auth_service.entity.RefreshToken;
import com.aerospace.auth_service.entity.Role;
import com.aerospace.auth_service.entity.User;
import com.aerospace.auth_service.repository.RefreshTokenRepository;
import com.aerospace.auth_service.repository.RoleRepository;
import com.aerospace.auth_service.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // need this for role lookup
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email is already registered");
        }

        // Public self-registration always gets the default role — client-supplied
        // roles are never honored here (see AGENT.md security note: this endpoint
        // is permitAll(), so trusting client-supplied roles is a privilege-escalation hole).
        Set<Role> roles = new HashSet<>();
        Role defaultRole = roleRepository.findByName("STORE_KEEPER")
                .orElseThrow(() -> new IllegalStateException("Default role STORE_KEEPER not found"));
        roles.add(defaultRole);


        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .roles(roles)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("👤 Registered new user: {} with roles: {}", savedUser.getUsername(),
                savedUser.getRoles().stream().map(Role::getName).collect(Collectors.joining(",")));

        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .roles(savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(savedUser.getEnabled())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "Account temporarily locked due to repeated failed login attempts. Try again after "
                            + user.getLockedUntil() + ".");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            registerFailedLogin(user);
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.getEnabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getUsername(), user.getEmail(), roleNames, user.getSupplierId());
        String refreshToken = createRefreshToken(user, LocalDateTime.now());
        log.info("🔐 User logged in: {} with roles: {}", user.getUsername(), roleNames);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.isExpired()) {
            throw new IllegalStateException("Refresh token is not valid");
        }

        // Absolute session cap: sessionStartAt is carried forward unchanged through
        // every rotation, so this measures time since the ORIGINAL login, not since the
        // last refresh — an active user still gets forced back to a real login after
        // refreshTokenExpirationMs, instead of extending indefinitely.
        LocalDateTime sessionStartAt = storedToken.getSessionStartAt() != null
                ? storedToken.getSessionStartAt() : storedToken.getExpiryDate().minusSeconds(refreshTokenExpirationMs / 1000);
        if (sessionStartAt.plusSeconds(refreshTokenExpirationMs / 1000).isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new IllegalStateException("Session expired — please log in again.");
        }

        User user = storedToken.getUser();
        if (user == null || !user.getEnabled()) {
            throw new IllegalStateException("Refresh token owner is not available");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String token = jwtService.generateToken(user.getUsername(), user.getEmail(), roleNames);
        String refreshToken = createRefreshToken(user, sessionStartAt);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }

    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long ACCOUNT_LOCK_MINUTES = 15;

    private void registerFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        attempts++;
        if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES));
            user.setFailedLoginAttempts(0);
        } else {
            user.setFailedLoginAttempts(attempts);
        }
        userRepository.save(user);
    }

    private String createRefreshToken(User user, LocalDateTime sessionStartAt) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .sessionStartAt(sessionStartAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    public TokenValidationResponse validateToken(TokenValidationRequest request) {
        try {
            String token = request.getToken();
            String username = jwtService.extractUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);

            if (user != null && jwtService.isTokenValid(token, user.getUsername())) {
                Claims claims = jwtService.extractAllClaims(token);
                // Extract roles list from claim in a type-safe manner
                List<?> rawRoles = claims.get("roles", List.class);
                Set<String> rolesSet = rawRoles == null ? new HashSet<>()
                        : rawRoles.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .collect(Collectors.toSet());

                return TokenValidationResponse.builder()
                        .valid(true)
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .roles(rolesSet)
                        .build();
            }
        } catch (Exception e) {
            log.error("Token validation failed", e);
        }

        return TokenValidationResponse.builder()
                .valid(false)
                .build();
    }

    @Transactional
    public UserResponse createUserWithRoles(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email is already registered");
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            roles.add(role);
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .roles(roles)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Admin created user: {} with roles: {}", savedUser.getUsername(),
                savedUser.getRoles().stream().map(Role::getName).collect(Collectors.joining(",")));

        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .roles(savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(savedUser.getEnabled())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    public List<UserResponse> listAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId()).username(u.getUsername()).email(u.getEmail())
                        .roles(u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                        .enabled(u.getEnabled()).createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public UserResponse setUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        return UserResponse.builder()
                .id(saved.getId()).username(saved.getUsername()).email(saved.getEmail())
                .roles(saved.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(saved.getEnabled()).createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public UserResponse updateUserRoles(Long id, List<String> roleNames) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        Set<Role> roles = new HashSet<>();
        if (roleNames != null) {
            for (String roleName : roleNames) {
                if (roleName == null || roleName.isBlank()) {
                    continue;
                }
                Role role = roleRepository.findByName(roleName.trim())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                roles.add(role);
            }
        }

        user.setRoles(roles);
        User saved = userRepository.save(user);
        log.info("Admin updated roles for user: {} -> {}", saved.getUsername(),
                saved.getRoles().stream().map(Role::getName).collect(Collectors.joining(",")));
        return UserResponse.builder()
                .id(saved.getId()).username(saved.getUsername()).email(saved.getEmail())
                .roles(saved.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(saved.getEnabled()).createdAt(saved.getCreatedAt())
                .build();
    }
}