package com.portfolio.controller;

import com.portfolio.domain.AuditLog;
import com.portfolio.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Controller - migrated from COBOL SECMGR audit functionality
 * REST API for audit log inquiries
 */
@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAudits() {
        return ResponseEntity.ok(auditService.getAllAudits());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(auditService.getAuditsByUser(userId));
    }

    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<Page<AuditLog>> getAuditsByUserPaged(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getAuditsByUserPaged(userId, page, size));
    }

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<AuditLog>> getAuditsByPortfolio(@PathVariable String portfolioId) {
        return ResponseEntity.ok(auditService.getAuditsByPortfolio(portfolioId));
    }

    @GetMapping("/range")
    public ResponseEntity<List<AuditLog>> getAuditsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(auditService.getAuditsByDateRange(startTime, endTime));
    }

    @GetMapping("/failed")
    public ResponseEntity<List<AuditLog>> getFailedOperations() {
        return ResponseEntity.ok(auditService.getFailedOperations());
    }

    @GetMapping("/login-attempts/{userId}")
    public ResponseEntity<Long> getLoginAttemptCount(
            @PathVariable String userId,
            @RequestParam(defaultValue = "24") int hoursBack) {
        return ResponseEntity.ok(auditService.getLoginAttemptCount(userId, hoursBack));
    }
}
