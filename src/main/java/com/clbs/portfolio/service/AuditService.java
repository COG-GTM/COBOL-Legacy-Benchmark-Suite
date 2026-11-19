package com.clbs.portfolio.service;

import com.clbs.portfolio.domain.AuditRecord;
import com.clbs.portfolio.repository.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Port of AUDPROC: appends records to the audit trail. */
@Service
public class AuditService {

    public static final String SYSTEM_ID = "PORTFOLIO";
    public static final String TYPE_TRANSACTION = "TRAN";
    public static final String TYPE_USER = "USER";
    public static final String STATUS_SUCCESS = "SUCC";
    public static final String STATUS_FAILURE = "FAIL";

    private final AuditRepository auditRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    public AuditService(AuditRepository auditRepository, CurrentUser currentUser, Clock clock) {
        this.auditRepository = auditRepository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditRecord record(String program, String type, String action, String status, String portfolioId,
                              String accountNo, String reasonCode, String beforeImage, String afterImage,
                              String message) {
        return auditRepository.save(new AuditRecord(LocalDateTime.now(clock), SYSTEM_ID, currentUser.name(),
                program, type, action, status, portfolioId, accountNo, reasonCode, beforeImage, afterImage,
                message));
    }

    @Transactional(readOnly = true)
    public List<AuditRecord> find(String portfolioId) {
        return portfolioId == null
                ? auditRepository.findAllByOrderByIdAsc()
                : auditRepository.findByPortfolioIdOrderByIdAsc(portfolioId);
    }
}
