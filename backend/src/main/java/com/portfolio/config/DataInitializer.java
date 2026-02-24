package com.portfolio.config;

import com.portfolio.entity.AppUser;
import com.portfolio.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Data initializer - ensures default users exist with properly encoded passwords.
 * Replaces RACF user provisioning on z/OS.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initUsers(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("password"));
                admin.setEmail("admin@portfolio.com");
                admin.setFullName("System Admin");
                admin.setRole("ADMIN");
                admin.setEnabled(true);
                userRepository.save(admin);

                AppUser user1 = new AppUser();
                user1.setUsername("user1");
                user1.setPassword(passwordEncoder.encode("password"));
                user1.setEmail("user1@portfolio.com");
                user1.setFullName("Portfolio Analyst");
                user1.setRole("USER");
                user1.setEnabled(true);
                userRepository.save(user1);

                log.info("Default users created: admin, user1 (password: password)");
            }
        };
    }
}
