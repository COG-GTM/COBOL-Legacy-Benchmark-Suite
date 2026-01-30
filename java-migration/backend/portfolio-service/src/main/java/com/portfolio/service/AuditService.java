package com.portfolio.service;

import com.portfolio.model.entity.AuditLog;
import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.entity.Transaction;
import com.portfolio.model.enums.AuditAction;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.model.enums.AuditType;
import com.portfolio.model.enums.TransactionType;
import com.portfolio.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog createTransactionAudit(Transaction transaction, Portfolio portfolio, 
                                           String beforeImage, AuditStatus status, String message) {
        AuditAction action = mapTransactionTypeToAction(transaction.getType());
        
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .systemId("PORTSERV")
                .userId(transaction.getProcessUser())
                .program("PORTTRAN")
                .type(AuditType.TRANSACTION)
                .action(action)
                .status(status)
                .portfolioId(transaction.getPortfolioId())
                .accountNo(portfolio != null ? portfolio.getAccountNo() : null)
                .beforeImage(beforeImage)
                .afterImage(buildAfterImage(transaction))
                .message(buildAuditMessage(transaction, message))
                .build();
        
        return auditLogRepository.save(auditLog);
    }

    @Transactional
    public AuditLog createPortfolioUpdateAudit(Portfolio portfolio, String beforeImage, 
                                                String userId, AuditStatus status, String message) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .systemId("PORTSERV")
                .userId(userId)
                .program("PORTUPDT")
                .type(AuditType.USER_ACTION)
                .action(AuditAction.UPDATE)
                .status(status)
                .portfolioId(portfolio.getPortfolioId())
                .accountNo(portfolio.getAccountNo())
                .beforeImage(beforeImage)
                .afterImage(buildPortfolioImage(portfolio))
                .message(message)
                .build();
        
        return auditLogRepository.save(auditLog);
    }

    @Transactional
    public AuditLog createInquiryAudit(String portfolioId, String accountNo, 
                                       String userId, AuditStatus status, String message) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .systemId("PORTSERV")
                .userId(userId)
                .program("INQPORT")
                .type(AuditType.USER_ACTION)
                .action(AuditAction.INQUIRE)
                .status(status)
                .portfolioId(portfolioId)
                .accountNo(accountNo)
                .message(message)
                .build();
        
        return auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getAuditLogsByPortfolioId(String portfolioId) {
        return auditLogRepository.findByPortfolioId(portfolioId);
    }

    public List<AuditLog> getAuditLogsByAccountNo(String accountNo) {
        return auditLogRepository.findByAccountNo(accountNo);
    }

    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogRepository.findByTimestampBetween(startTime, endTime);
    }

    private AuditAction mapTransactionTypeToAction(TransactionType type) {
        return switch (type) {
            case BUY -> AuditAction.CREATE;
            case SELL -> AuditAction.DELETE;
            case TRANSFER, FEE -> AuditAction.UPDATE;
        };
    }

    private String buildAuditMessage(Transaction transaction, String additionalMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction: ").append(transaction.getType().getCode());
        sb.append(" Amount: ").append(transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO);
        sb.append(" Units: ").append(transaction.getQuantity());
        if (additionalMessage != null && !additionalMessage.isEmpty()) {
            sb.append(" - ").append(additionalMessage);
        }
        return sb.toString();
    }

    private String buildAfterImage(Transaction transaction) {
        return String.format("Type=%s,Qty=%s,Price=%s,Amount=%s",
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getPrice(),
                transaction.getAmount());
    }

    private String buildPortfolioImage(Portfolio portfolio) {
        return String.format("ID=%s,Status=%s,Name=%s,Value=%s,Units=%s,Cost=%s",
                portfolio.getPortfolioId(),
                portfolio.getStatus(),
                portfolio.getClientName(),
                portfolio.getTotalValue(),
                portfolio.getTotalUnits(),
                portfolio.getTotalCost());
    }
}
