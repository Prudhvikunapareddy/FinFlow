package com.finflow.auth_service.CONFIG;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.finflow.auth_service.Entity.User;
import com.finflow.auth_service.REPOSITORY.UserRepository;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    CommandLineRunner createDefaultAdmin(UserRepository userRepository,
                                         PasswordEncoder passwordEncoder,
                                         @Value("${auth.bootstrap.admin.enabled:true}") boolean enabled,
                                         @Value("${auth.bootstrap.admin.email:admin@finflow.com}") String adminEmail,
                                         @Value("${auth.bootstrap.admin.password:Admin@123}") String adminPassword) {
        return args -> {
            if (!enabled) {
                return;
            }

            String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
            userRepository.findByEmail(normalizedEmail).ifPresentOrElse(existing -> {
                if (!"ADMIN".equalsIgnoreCase(existing.getRole())) {
                    existing.setRole("ADMIN");
                    userRepository.save(existing);
                }
            }, () -> {
                User admin = new User();
                admin.setEmail(normalizedEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                userRepository.save(admin);
            });
        };
    }
}
