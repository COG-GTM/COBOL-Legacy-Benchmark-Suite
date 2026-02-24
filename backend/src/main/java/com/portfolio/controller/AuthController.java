package com.portfolio.controller;

import com.portfolio.dto.AuthRequest;
import com.portfolio.dto.AuthResponse;
import com.portfolio.entity.AppUser;
import com.portfolio.repository.AppUserRepository;
import com.portfolio.security.JwtTokenProvider;
import com.portfolio.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

/**
 * Authentication controller - replaces SECMGR user validation and session control.
 * Source: src/programs/online/SECMGR.cbl P100-VALIDATE-USER
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          AppUserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                               HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        AppUser user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        auditService.logLogin(user.getUsername(), httpRequest.getRemoteAddr(), true);

        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request,
                                                  HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().build();
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setEnabled(true);
        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        auditService.logLogin(user.getUsername(), httpRequest.getRemoteAddr(), true);

        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole()));
    }
}
