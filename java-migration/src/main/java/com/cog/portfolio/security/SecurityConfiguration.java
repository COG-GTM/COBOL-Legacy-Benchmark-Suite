package com.cog.portfolio.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Replaces SECMGR.cbl. Authorization in this demo is role based only:
 * {@link Roles#ADMIN}, {@link Roles#OPERATOR}, {@link Roles#USER}.
 *
 * <p>The legacy {@code SELECT COUNT(*) FROM AUTHFILE WHERE USER_ID = ? AND RESOURCE = ?}
 * (per-resource authorization) is intentionally NOT migrated.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    public static final class Roles {
        public static final String ADMIN = "admin";
        public static final String OPERATOR = "operator";
        public static final String USER = "user";

        private Roles() {
        }
    }

    // TODO producción: la autorización granular por recurso basada en AUTHFILE (SECMGR.cbl) queda
    // pendiente. Decidir si AUTHFILE se migra como tabla de permisos / UserDetailsService o si los
    // roles admin/operator/user son suficientes. En la demo solo se aplican roles.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/error", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/h2-console/**").hasRole(Roles.ADMIN)
                        .requestMatchers("/actuator/**").hasAnyRole(Roles.OPERATOR, Roles.ADMIN)
                        .requestMatchers("/batch/**", "/maintenance/**", "/validation/**")
                        .hasAnyRole(Roles.OPERATOR, Roles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/portfolios/**")
                        .hasAnyRole(Roles.USER, Roles.OPERATOR, Roles.ADMIN)
                        .requestMatchers("/api/portfolios/**").hasAnyRole(Roles.OPERATOR, Roles.ADMIN)
                        .requestMatchers("/", "/menu", "/inquiry/**")
                        .hasAnyRole(Roles.USER, Roles.OPERATOR, Roles.ADMIN)
                        .anyRequest().authenticated())
                .httpBasic(basic -> {
                })
                .formLogin(form -> form.defaultSuccessUrl("/menu", true))
                .logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout")))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/batch/**", "/maintenance/**",
                        "/validation/**", "/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Demo users only. TODO producción: reemplazar por un UserDetailsService real (ver nota AUTHFILE). */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode("admin")).roles(Roles.ADMIN).build(),
                User.withUsername("operator").password(encoder.encode("operator")).roles(Roles.OPERATOR).build(),
                User.withUsername("user").password(encoder.encode("user")).roles(Roles.USER).build());
    }
}
