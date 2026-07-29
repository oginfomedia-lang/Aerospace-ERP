package com.aerospace.auth_service.config;

import com.aerospace.auth_service.entity.Role;
import com.aerospace.auth_service.entity.User;
import com.aerospace.auth_service.repository.RoleRepository;
import com.aerospace.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final List<String> ALL_ROLES = List.of(
            "STORE_KEEPER", "QA_INSPECTOR", "PRODUCTION_ENGINEER",
            "PROCUREMENT_OFFICER", "MANAGER", "ADMIN", "SUPPLIER"
    );

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:admin}")
    private String bootstrapAdminUsername;

    @Value("${app.bootstrap-admin.email:admin@example.com}")
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap-admin.password:}")
    private String bootstrapAdminPassword;

    @Override
    public void run(String... args) {
        seedRoles();
        seedBootstrapAdmin();
    }

    private void seedRoles() {
        ALL_ROLES.forEach(name -> {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(name));
                log.info("Seeded role: {}", name);
            }
        });
    }

    private void seedBootstrapAdmin() {
        boolean adminExists = userRepository.findByUsername(bootstrapAdminUsername).isPresent();
        if (adminExists) {
            return;
        }
        if (bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank()) {
            log.warn("No ADMIN user exists and app.bootstrap-admin.password is not set — " +
                    "set the ADMIN_BOOTSTRAP_PASSWORD env var to auto-create the first admin.");
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing after seeding"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);

        User admin = User.builder()
                .username(bootstrapAdminUsername)
                .email(bootstrapAdminEmail)
                .password(passwordEncoder.encode(bootstrapAdminPassword))
                .roles(roles)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Bootstrapped initial ADMIN user '{}' — change this password immediately after first login.",
                bootstrapAdminUsername);
    }
}