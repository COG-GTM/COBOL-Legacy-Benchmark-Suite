package com.clbs.online.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase 0 security baseline. Read-only inquiry endpoints are open; this will be
 * tightened to mirror the COBOL SECMGR / RACF model in later phases.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**", "/actuator/**").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
