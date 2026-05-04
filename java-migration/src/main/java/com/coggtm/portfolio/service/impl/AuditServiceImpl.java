package com.coggtm.portfolio.service.impl;

import com.coggtm.portfolio.domain.AuditRecord;
import com.coggtm.portfolio.domain.enums.AuditAction;
import com.coggtm.portfolio.domain.enums.AuditType;
import com.coggtm.portfolio.repository.AuditRepository;
import com.coggtm.portfolio.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditRepository auditRepository;

    public AuditServiceImpl(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public AuditRecord logTransaction(String portfolioId, String action,
                                      String beforeImage, String afterImage) {
        // TODO: Migrate from AUDPROC.cbl transaction logging path
        AuditRecord record = AuditRecord.builder()
                .timestamp(LocalDateTime.now())
                .auditType(AuditType.TRAN)
                .action(AuditAction.valueOf(action))
                .portfolioId(portfolioId)
                .beforeImage(beforeImage)
                .afterImage(afterImage)
                .status("SUCC")
                .build();
        return auditRepository.save(record);
    }

    @Override
    public AuditRecord logUserAction(String userId, String action, String message) {
        // TODO: Migrate from AUDPROC.cbl user action logging path
        AuditRecord record = AuditRecord.builder()
                .timestamp(LocalDateTime.now())
                .auditType(AuditType.USER)
                .action(AuditAction.valueOf(action))
                .userId(userId)
                .message(message)
                .status("SUCC")
                .build();
        return auditRepository.save(record);
    }

    @Override
    public AuditRecord logSystemEvent(String systemId, String action, String message) {
        // TODO: Migrate from AUDPROC.cbl system event logging path
        AuditRecord record = AuditRecord.builder()
                .timestamp(LocalDateTime.now())
                .auditType(AuditType.SYST)
                .action(AuditAction.valueOf(action))
                .systemId(systemId)
                .message(message)
                .status("SUCC")
                .build();
        return auditRepository.save(record);
    }
}
