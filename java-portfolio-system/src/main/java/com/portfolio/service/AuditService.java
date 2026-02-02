package com.portfolio.service;

import com.portfolio.domain.AuditLog;
import com.portfolio.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Service - migrated from COBOL SECMGR audit functionality
 * Handles security and access audit trail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String userId, String program, AuditLog.AuditType type,
                          AuditLog.AuditAction action, AuditLog.AuditStatus status,
                          String portfolioId, String message) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .systemId("PORTSYS")
                .userId(userId)
                .program(program)
                .auditType(type)
                .action(action)
                .status(status)
                .portfolioId(portfolioId)
                .message(message)
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Audit logged: user={}, action={}, status={}", userId, action, status);
    }

    public void logLogin(String userId, boolean success) {
        logAction(userId, "SECMGR", AuditLog.AuditType.USER,
                AuditLog.AuditAction.LOGIN,
                success ? AuditLog.AuditStatus.SUCC : AuditLog.AuditStatus.FAIL,
                null, success ? "Login successful" : "Login failed");
    }

    public void logLogout(String userId) {
        logAction(userId, "SECMGR", AuditLog.AuditType.USER,
                AuditLog.AuditAction.LOGOUT, AuditLog.AuditStatus.SUCC,
                null, "Logout successful");
    }

    public void logInquiry(String userId, String portfolioId) {
        logAction(userId, "INQONLN", AuditLog.AuditType.TRAN,
                AuditLog.AuditAction.INQUIRE, AuditLog.AuditStatus.SUCC,
                portfolioId, "Portfolio inquiry");
    }

    public void logTransaction(String userId, String portfolioId, String transactionType, boolean success) {
        logAction(userId, "POSUPD", AuditLog.AuditType.TRAN,
                success ? AuditLog.AuditAction.CREATE : AuditLog.AuditAction.UPDATE,
                success ? AuditLog.AuditStatus.SUCC : AuditLog.AuditStatus.FAIL,
                portfolioId, "Transaction: " + transactionType);
    }

    public List<AuditLog> getAuditsByUser(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public Page<AuditLog> getAuditsByUserPaged(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    public List<AuditLog> getAuditsByPortfolio(String portfolioId) {
        return auditLogRepository.findByPortfolioId(portfolioId);
    }

    public List<AuditLog> getAuditsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByTimestampBetween(startTime, endTime);
    }

    public List<AuditLog> getFailedOperations() {
        return auditLogRepository.findFailedOperations();
    }

    public long getLoginAttemptCount(String userId, int hoursBack) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        return auditLogRepository.countLoginAttempts(userId, since);
    }

    public List<AuditLog> getAllAudits() {
        return auditLogRepository.findAll();
    }
}
