package com.coggtm.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration placeholder.
 * <p>
 * Replaces COBOL security manager {@code SECMGR.cbl} and CICS
 * transaction-level security. The current configuration permits
 * all requests to support development; production hardening is a
 * subsequent migration task.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // TODO: Implement role-based access control matching SECMGR.cbl rules
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
