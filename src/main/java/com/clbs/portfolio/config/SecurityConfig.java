package com.clbs.portfolio.config;

import com.clbs.portfolio.exception.ErrorCode;
import com.clbs.portfolio.web.ErrorResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    public static final String ROLE_VIEWER = "VIEWER";
    public static final String ROLE_OPERATOR = "OPERATOR";

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Stateless HTTP Basic API: no session cookie is issued, so CSRF protection is not needed. Everything is
     * authenticated except health/info probes and the OpenAPI documentation.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ErrorResponseWriter errors) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").hasRole(ROLE_OPERATOR)
                        .requestMatchers("/api/v1/batch/**", "/api/v1/audit/**").hasRole(ROLE_OPERATOR)
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole(ROLE_VIEWER, ROLE_OPERATOR)
                        .requestMatchers(HttpMethod.POST, "/api/v1/validations").hasAnyRole(ROLE_VIEWER, ROLE_OPERATOR)
                        .requestMatchers("/api/v1/**").hasRole(ROLE_OPERATOR)
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setHeader("WWW-Authenticate", "Basic realm=\"clbs\"");
                            errors.write(req, res, HttpStatus.UNAUTHORIZED, ErrorCode.SECURITY,
                                    "Authentication required");
                        })
                        .accessDeniedHandler((req, res, ex) ->
                                errors.write(req, res, HttpStatus.FORBIDDEN, ErrorCode.SECURITY, "Access denied")));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(SecurityProperties props, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(props.viewerUsername())
                        .password(encoder.encode(password(props.viewerUsername(), props.viewerPassword(), props)))
                        .roles(ROLE_VIEWER).build(),
                User.withUsername(props.operatorUsername())
                        .password(encoder.encode(password(props.operatorUsername(), props.operatorPassword(), props)))
                        .roles(ROLE_OPERATOR).build());
    }

    private static String password(String user, String configured, SecurityProperties props) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        if (props.requirePasswords()) {
            throw new IllegalStateException("No password configured for API user '" + user + "'");
        }
        String generated = UUID.randomUUID().toString();
        log.warn("No password configured for API user '{}'; generated one for this run: {}", user, generated);
        return generated;
    }
}
