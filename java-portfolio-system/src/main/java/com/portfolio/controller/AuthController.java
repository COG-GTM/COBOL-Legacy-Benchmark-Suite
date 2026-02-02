package com.portfolio.controller;

import com.portfolio.domain.AuditLog;
import com.portfolio.domain.User;
import com.portfolio.repository.UserRepository;
import com.portfolio.security.JwtService;
import com.portfolio.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Authentication Controller - migrated from COBOL SECMGR
 * Handles user authentication and registration
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            User user = userRepository.findById(request.getUserId()).orElse(null);
            if (user != null) {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
            }

            auditService.logLogin(request.getUserId(), true);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", request.getUserId());
            response.put("message", "Login successful");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            auditService.logLogin(request.getUserId(), false);
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Authentication failed");
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsById(request.getUserId())) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User already exists");
            return ResponseEntity.badRequest().body(response);
        }

        User user = User.builder()
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .status(User.UserStatus.A)
                .roles(Set.of(User.UserRole.USER))
                .build();

        userRepository.save(user);

        auditService.logAction(request.getUserId(), "SECMGR", AuditLog.AuditType.USER,
                AuditLog.AuditAction.CREATE, AuditLog.AuditStatus.SUCC, null, "User registered");

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("message", "User registered successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        String userId = "unknown";
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userId = jwtService.extractUsername(token);
        }

        auditService.logLogout(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logout successful");

        return ResponseEntity.ok(response);
    }

    public static class LoginRequest {
        private String userId;
        private String password;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String userId;
        private String password;
        private String fullName;
        private String email;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
