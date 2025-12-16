package com.portfolio.service;

import com.portfolio.entity.AuditLog;
import com.portfolio.entity.AuditLog.AuditAction;
import com.portfolio.entity.AuditLog.AuditEventType;
import com.portfolio.entity.AuditLog.AuditStatus;
import com.portfolio.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service for Audit logging operations.
 * Replaces COBOL SECMGR P300-LOG-ACCESS paragraph functionality.
 * 
 * @see src/programs/online/SECMGR.cbl - P300-LOG-ACCESS
 * @see src/copybook/common/AUDITLOG.cpy
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPortfolioAction(String portfolioId, String action, String userId, 
                                   String beforeImage, String afterImage) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .portfolioId(portfolioId)
                .eventType(AuditEventType.TRANSACTION)
                .action(mapAction(action))
                .status(AuditStatus.SUCCESS)
                .beforeImage(beforeImage)
                .afterImage(afterImage)
                .programId("PORTMGMT")
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Audit logged: portfolio={}, action={}, user={}", portfolioId, action, userId);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPositionAction(String portfolioId, String investmentId, String action, String userId) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .portfolioId(portfolioId)
                .eventType(AuditEventType.TRANSACTION)
                .action(mapAction(action))
                .status(AuditStatus.SUCCESS)
                .message("Investment: " + investmentId)
                .programId("POSMGMT")
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Audit logged: portfolio={}, investment={}, action={}", portfolioId, investmentId, action);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransactionAction(String transactionId, String action, String userId) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .eventType(AuditEventType.TRANSACTION)
                .action(mapAction(action))
                .status(AuditStatus.SUCCESS)
                .message("Transaction: " + transactionId)
                .programId("TRNMGMT")
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Audit logged: transaction={}, action={}", transactionId, action);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserAction(String userId, AuditAction action, AuditStatus status, String message) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .eventType(AuditEventType.USER_ACTION)
                .action(action)
                .status(status)
                .message(message)
                .programId("SECMGMT")
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Audit logged: user={}, action={}, status={}", userId, action, status);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSystemEvent(String programId, AuditAction action, AuditStatus status, String message) {
        AuditLog auditLog = AuditLog.builder()
                .userId("SYSTEM")
                .eventType(AuditEventType.SYSTEM_EVENT)
                .action(action)
                .status(status)
                .message(message)
                .programId(programId)
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("System audit logged: program={}, action={}", programId, action);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByUserId(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByDateRange(OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable) {
        return auditLogRepository.findByAuditTimestampBetween(startTime, endTime, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findRecentFailures(int hours) {
        OffsetDateTime startTime = OffsetDateTime.now().minusHours(hours);
        return auditLogRepository.findRecentFailures(startTime);
    }

    @Transactional(readOnly = true)
    public long countLoginsSince(OffsetDateTime startTime) {
        return auditLogRepository.countByActionSince(AuditAction.LOGIN, startTime);
    }

    private AuditAction mapAction(String action) {
        return switch (action.toUpperCase()) {
            case "CREATE" -> AuditAction.CREATE;
            case "UPDATE" -> AuditAction.UPDATE;
            case "DELETE" -> AuditAction.DELETE;
            case "INQUIRE", "READ" -> AuditAction.INQUIRE;
            case "LOGIN" -> AuditAction.LOGIN;
            case "LOGOUT" -> AuditAction.LOGOUT;
            case "CLOSE", "PROCESS", "REVERSE", "FAIL" -> AuditAction.UPDATE;
            default -> AuditAction.INQUIRE;
        };
    }
}
