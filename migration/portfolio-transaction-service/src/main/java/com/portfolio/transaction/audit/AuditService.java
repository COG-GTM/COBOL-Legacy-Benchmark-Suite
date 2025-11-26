package com.portfolio.transaction.audit;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.entity.Portfolio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditRecordRepository auditRepository;

    public AuditService(AuditRecordRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransaction(TransactionRequest request,
                                  Portfolio beforeImage,
                                  Portfolio afterImage) {
        AuditRecord audit = new AuditRecord();

        audit.setTimestamp(LocalDateTime.now());
        audit.setProgram("PORTTRAN");
        audit.setAuditType("TRAN");

        String action = mapTransactionTypeToAction(request.getTransactionType());
        audit.setAction(action);

        audit.setStatus("SUCC");
        audit.setPortfolioId(request.getPortfolioId());
        audit.setAccountNo(afterImage.getAccountNo());

        audit.setBeforeImage(serializePortfolio(beforeImage));
        audit.setAfterImage(serializePortfolio(afterImage));

        String message = String.format(
            "Transaction: %s Amount: %s Units: %s",
            request.getTransactionType(),
            request.getAmount(),
            request.getQuantity());
        audit.setMessage(message);

        auditRepository.save(audit);
    }

    private String mapTransactionTypeToAction(String type) {
        return switch (type.toUpperCase()) {
            case "BU" -> "CREATE";
            case "SL" -> "DELETE";
            case "TR" -> "UPDATE";
            case "FE" -> "UPDATE";
            default -> "UNKNOWN";
        };
    }

    private String serializePortfolio(Portfolio portfolio) {
        if (portfolio == null) {
            return null;
        }
        return String.format("ID:%s,ACCT:%s,UNITS:%s,COST:%s",
            portfolio.getPortfolioId(),
            portfolio.getAccountNo(),
            portfolio.getTotalUnits(),
            portfolio.getTotalCost());
    }
}
