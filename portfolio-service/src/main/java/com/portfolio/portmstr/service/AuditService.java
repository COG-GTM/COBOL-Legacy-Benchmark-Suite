package com.portfolio.portmstr.service;

import com.portfolio.portmstr.model.AuditLog;
import com.portfolio.portmstr.model.PortfolioMaster;
import com.portfolio.portmstr.model.enums.AuditAction;
import com.portfolio.portmstr.repository.AuditLogRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Audit logging service.
 * Replaces COBOL CALL 'AUDPROC' USING LS-AUDIT-REQUEST
 * from PORTMSTR.cbl 2100-LOG-PORTFOLIO-UPDATE paragraph.
 */
@Service
public class AuditService {

    private static final String SYSTEM_ID = "PORTFOL";
    private static final String PROGRAM_NAME = "PORTMSTR";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logPortfolioCreate(PortfolioMaster portfolio, String userId) {
        AuditLog audit = buildAuditLog(AuditAction.CREATE, portfolio, userId);
        audit.setAfterImage(portfolioToString(portfolio));
        audit.setMessage("Portfolio created successfully");
        auditLogRepository.save(audit);
    }

    public void logPortfolioUpdate(PortfolioMaster beforeImage, PortfolioMaster afterImage, String userId) {
        AuditLog audit = buildAuditLog(AuditAction.UPDATE, afterImage, userId);
        audit.setBeforeImage(portfolioToString(beforeImage));
        audit.setAfterImage(portfolioToString(afterImage));
        audit.setMessage("Portfolio updated successfully");
        auditLogRepository.save(audit);
    }

    public void logPortfolioDelete(PortfolioMaster portfolio, String userId, String reasonCode) {
        AuditLog audit = buildAuditLog(AuditAction.DELETE, portfolio, userId);
        audit.setBeforeImage(portfolioToString(portfolio));
        audit.setMessage("Portfolio deleted. Reason: " + reasonCode);
        auditLogRepository.save(audit);
    }

    public void logTransaction(String portfolioId, String accountNo, String transactionType,
                               String amount, String userId) {
        AuditLog audit = new AuditLog();
        audit.setAuditTimestamp(LocalDateTime.now());
        audit.setSystemId(SYSTEM_ID);
        audit.setUserId(userId);
        audit.setProgramName("PORTTRAN");
        audit.setAuditType("TRAN");
        audit.setAuditAction(AuditAction.CREATE);
        audit.setAuditStatus("SUCC");
        audit.setPortfolioId(portfolioId);
        audit.setAccountNo(accountNo);
        audit.setMessage("Transaction: " + transactionType + " Amount: " + amount);
        auditLogRepository.save(audit);
    }

    private AuditLog buildAuditLog(AuditAction action, PortfolioMaster portfolio, String userId) {
        AuditLog audit = new AuditLog();
        audit.setAuditTimestamp(LocalDateTime.now());
        audit.setSystemId(SYSTEM_ID);
        audit.setUserId(userId);
        audit.setProgramName(PROGRAM_NAME);
        audit.setAuditType("TRAN");
        audit.setAuditAction(action);
        audit.setAuditStatus("SUCC");
        audit.setPortfolioId(portfolio.getPortfolioId());
        audit.setAccountNo(portfolio.getAccountNo());
        return audit;
    }

    private String portfolioToString(PortfolioMaster portfolio) {
        return String.format("ID=%s,ACCT=%s,NAME=%s,STATUS=%s,VALUE=%s",
                portfolio.getPortfolioId(),
                portfolio.getAccountNo(),
                portfolio.getClientName(),
                portfolio.getStatus(),
                portfolio.getTotalValue());
    }
}
