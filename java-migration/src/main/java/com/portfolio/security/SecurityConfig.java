package com.portfolio.security;

import com.portfolio.domain.AppUser;
import com.portfolio.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers("/login", "/css/**", "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(formLogin -> formLogin.defaultSuccessUrl("/inquiry/menu", true).permitAll())
        .httpBasic(httpBasic -> {})
        .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"))
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(AppUserRepository userRepository) {
    return username ->
        userRepository
            .findByUserId(username)
            .map(this::details)
            .orElseThrow(() -> new UsernameNotFoundException(username));
  }

  private UserDetails details(AppUser appUser) {
    return User.withUsername(appUser.getUserId())
        .password(appUser.getPasswordHash())
        .roles(appUser.getRole())
        .disabled(!appUser.isEnabled())
        .build();
  }
}
