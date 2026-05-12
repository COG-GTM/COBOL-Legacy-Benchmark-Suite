package com.portfolio.service.common;

import com.portfolio.model.entity.AuditRecord;
import com.portfolio.model.enums.AuditAction;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.model.enums.AuditType;
import com.portfolio.repository.AuditRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRecordRepository auditRecordRepository;

    public AuditService(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    @Transactional
    public void logTransaction(String portfolioId, String accountNo, AuditAction action,
                               AuditStatus status, String userId, String program,
                               String beforeImage, String afterImage, String message) {
        AuditRecord record = buildRecord(AuditType.TRANSACTION, action, status,
                userId, program, portfolioId, accountNo, beforeImage, afterImage, message);
        auditRecordRepository.save(record);
        log.info("Audit: {} {} portfolio={} user={}", action, status, portfolioId, userId);
    }

    @Transactional
    public void logUserAction(AuditAction action, AuditStatus status, String userId,
                              String program, String message) {
        AuditRecord record = buildRecord(AuditType.USER_ACTION, action, status,
                userId, program, null, null, null, null, message);
        auditRecordRepository.save(record);
        log.info("Audit: user action {} {} user={}", action, status, userId);
    }

    @Transactional
    public void logSystemEvent(AuditAction action, AuditStatus status, String program,
                               String message) {
        AuditRecord record = buildRecord(AuditType.SYSTEM_EVENT, action, status,
                "SYSTEM", program, null, null, null, null, message);
        auditRecordRepository.save(record);
        log.info("Audit: system event {} {} program={}", action, status, program);
    }

    @Transactional
    public void logAccess(String userId, String resource, AuditStatus status) {
        AuditRecord record = buildRecord(AuditType.USER_ACTION, AuditAction.INQUIRE,
                status, userId, "SECMGR", null, null, null, null,
                "Access: " + resource);
        auditRecordRepository.save(record);
    }

    @Transactional
    public void logPortfolioUpdate(String portfolioId, String userId, String beforeImage) {
        logTransaction(portfolioId, null, AuditAction.UPDATE, AuditStatus.SUCCESS,
                userId, "PORTMSTR", beforeImage, null, "Portfolio updated");
    }

    private AuditRecord buildRecord(AuditType type, AuditAction action, AuditStatus status,
                                    String userId, String program, String portfolioId,
                                    String accountNo, String beforeImage, String afterImage,
                                    String message) {
        AuditRecord record = new AuditRecord();
        record.setAuditTimestamp(LocalDateTime.now());
        record.setSystemId("PORTFOLIO");
        record.setUserId(userId);
        record.setProgram(program);
        record.setAuditType(type.getCode());
        record.setAuditAction(action.getCode());
        record.setAuditStatus(status.getCode());
        record.setPortfolioId(portfolioId);
        record.setAccountNo(accountNo);
        record.setBeforeImage(beforeImage);
        record.setAfterImage(afterImage);
        record.setMessage(message);
        return record;
    }
}
