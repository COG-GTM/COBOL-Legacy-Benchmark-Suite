package com.portfolio.service;

import com.portfolio.repository.AuditRepository;
import com.portfolio.repository.BatchControlRepository;
import com.portfolio.repository.PositionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maintenance Service.
 * Replaces: UTLMNT00.cbl - Database maintenance operations
 * (cleanup old records, reorganize, etc.).
 */
@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final AuditRepository auditRepository;
    private final BatchControlRepository batchControlRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final AuditService auditService;

    @Value("${portfolio.maintenance.audit-retention-days:365}")
    private int auditRetentionDays;

    @Value("${portfolio.maintenance.batch-retention-days:90}")
    private int batchRetentionDays;

    public MaintenanceService(AuditRepository auditRepository,
                              BatchControlRepository batchControlRepository,
                              PositionHistoryRepository positionHistoryRepository,
                              AuditService auditService) {
        this.auditRepository = auditRepository;
        this.batchControlRepository = batchControlRepository;
        this.positionHistoryRepository = positionHistoryRepository;
        this.auditService = auditService;
    }

    /**
     * Performs full maintenance cycle.
     * Replaces the main processing loop in UTLMNT00.cbl.
     */
    @Transactional
    public MaintenanceResult performMaintenance() {
        log.info("Starting maintenance cycle");
        MaintenanceResult result = new MaintenanceResult();

        result.auditRecordsPurged = purgeOldAuditRecords();
        result.batchRecordsPurged = purgeOldBatchRecords();

        auditService.logSystemEvent("MAINTAIN", "SUCC",
                "Maintenance completed: audit=" + result.auditRecordsPurged
                        + ", batch=" + result.batchRecordsPurged);

        log.info("Maintenance cycle completed: {}", result);
        return result;
    }

    /**
     * Purges audit records older than retention period.
     */
    private int purgeOldAuditRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(auditRetentionDays);
        var oldRecords = auditRepository.findByAuditTimestampBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0), cutoff);
        int count = oldRecords.size();
        if (count > 0) {
            auditRepository.deleteAll(oldRecords);
            log.info("Purged {} audit records older than {} days", count, auditRetentionDays);
        }
        return count;
    }

    /**
     * Purges completed batch control records older than retention period.
     */
    private int purgeOldBatchRecords() {
        var doneRecords = batchControlRepository.findByStatus("D");
        String cutoffDate = LocalDate.now().minusDays(batchRetentionDays)
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        var oldRecords = doneRecords.stream()
                .filter(r -> r.getKey().getProcessDate().compareTo(cutoffDate) < 0)
                .toList();
        int count = oldRecords.size();
        if (count > 0) {
            batchControlRepository.deleteAll(oldRecords);
            log.info("Purged {} batch control records older than {} days", count, batchRetentionDays);
        }
        return count;
    }

    public static class MaintenanceResult {
        public int auditRecordsPurged;
        public int batchRecordsPurged;

        @Override
        public String toString() {
            return "MaintenanceResult{audit=" + auditRecordsPurged
                    + ", batch=" + batchRecordsPurged + "}";
        }
    }
}
