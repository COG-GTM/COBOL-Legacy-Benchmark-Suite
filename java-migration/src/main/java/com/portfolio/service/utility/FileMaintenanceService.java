package com.portfolio.service.utility;

import com.portfolio.repository.ErrorLogRepository;
import com.portfolio.service.common.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * File Maintenance Service - migrated from COBOL UTLMNT00.cbl.
 * VSAM reorganization -> database maintenance (vacuum/analyze).
 * Archiving -> scheduled cleanup jobs.
 */
@Service
public class FileMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(FileMaintenanceService.class);
    private static final int DEFAULT_RETENTION_DAYS = 90;

    private final ErrorLogRepository errorLogRepository;
    private final AuditService auditService;

    public FileMaintenanceService(ErrorLogRepository errorLogRepository,
                                  AuditService auditService) {
        this.errorLogRepository = errorLogRepository;
        this.auditService = auditService;
    }

    @Transactional
    public int purgeOldErrorLogs(int retentionDays) {
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        log.info("Purging error logs older than {}", cutoffDate);
        errorLogRepository.deleteByProcessDateBefore(cutoffDate);
        auditService.logSystemEvent("UTLMNT00", "CLEANUP", "SUCC",
                "Purged error logs older than " + cutoffDate);
        return 0;
    }

    @Transactional
    public int performMaintenance() {
        log.info("Starting database maintenance");
        purgeOldErrorLogs(DEFAULT_RETENTION_DAYS);
        log.info("Database maintenance completed");
        return 0;
    }
}
