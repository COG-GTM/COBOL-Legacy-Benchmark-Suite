package com.portfolio.controller;

import com.portfolio.model.entity.AuditLog;
import com.portfolio.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByPortfolio(@PathVariable String portfolioId) {
        log.info("Fetching audit logs for portfolio: {}", portfolioId);
        List<AuditLog> auditLogs = auditService.getAuditLogsByPortfolioId(portfolioId);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/account/{accountNo}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByAccount(@PathVariable String accountNo) {
        log.info("Fetching audit logs for account: {}", accountNo);
        List<AuditLog> auditLogs = auditService.getAuditLogsByAccountNo(accountNo);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        log.info("Fetching audit logs between {} and {}", startTime, endTime);
        List<AuditLog> auditLogs = auditService.getAuditLogsByDateRange(startTime, endTime);
        return ResponseEntity.ok(auditLogs);
    }
}
