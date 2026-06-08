package com.portfolio.infrastructure.audit;

import com.portfolio.domain.event.PortfolioCreatedEvent;
import com.portfolio.domain.event.TransactionProcessedEvent;
import com.portfolio.domain.model.AuditAction;
import com.portfolio.domain.model.AuditType;
import com.portfolio.domain.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for domain events and persists audit records.
 * Maps COBOL AUDPROC.cbl + PORTTRAN.cbl action mapping (lines 257-266).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String SYSTEM_ID = "PORTSVC";

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @TransactionalEventListener
    public void onTransactionProcessed(TransactionProcessedEvent event) {
        AuditAction action = mapTransactionTypeToAction(event.transactionType());

        AuditRecord record = AuditRecord.builder()
                .timestamp(event.timestamp())
                .systemId(SYSTEM_ID)
                .userId(event.userId())
                .program("PORTTRAN")
                .auditType(AuditType.TRANSACTION)
                .action(action)
                .status("SUCC")
                .portfolioId(event.portfolioId())
                .accountNumber(event.accountNumber())
                .message(event.transactionType().getCode() + " transaction: " + event.amount())
                .build();

        auditRepository.save(record);
        log.debug("Audit record created for transaction {} on portfolio {}",
                event.transactionType(), event.portfolioId());
    }

    @TransactionalEventListener
    public void onPortfolioCreated(PortfolioCreatedEvent event) {
        AuditRecord record = AuditRecord.builder()
                .timestamp(event.timestamp())
                .systemId(SYSTEM_ID)
                .userId(event.userId())
                .program("PORTMGMT")
                .auditType(AuditType.TRANSACTION)
                .action(AuditAction.CREATE)
                .status("SUCC")
                .portfolioId(event.portfolioId())
                .accountNumber(event.accountNumber())
                .message("Portfolio created")
                .build();

        auditRepository.save(record);
        log.debug("Audit record created for portfolio creation {}", event.portfolioId());
    }

    /**
     * Maps transaction type codes to audit actions per PORTTRAN.cbl lines 257-266:
     * BU -> CREATE, SL -> DELETE, TR -> UPDATE, FE -> UPDATE
     */
    static AuditAction mapTransactionTypeToAction(TransactionType type) {
        return switch (type) {
            case BUY -> AuditAction.CREATE;
            case SELL -> AuditAction.DELETE;
            case TRANSFER -> AuditAction.UPDATE;
            case FEE -> AuditAction.UPDATE;
        };
    }
}
