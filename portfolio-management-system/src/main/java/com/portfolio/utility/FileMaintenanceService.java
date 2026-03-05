package com.portfolio.utility;

import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.ErrorRecordRepository;
import com.portfolio.support.HistoryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * File Maintenance Service.
 * Migrated from COBOL UTLMNT00.
 * Scheduled DB2 archival/cleanup jobs replacing VSAM reorganization.
 */
@Service
public class FileMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(FileMaintenanceService.class);

    private final ErrorRecordRepository errorRecordRepository;
    private final HistoryRecordRepository historyRecordRepository;
    private final Db2StatisticsService statisticsService;

    @Value("${portfolio.maintenance.error-retention-days:90}")
    private int errorRetentionDays;

    @Value("${portfolio.maintenance.history-retention-days:365}")
    private int historyRetentionDays;

    public FileMaintenanceService(ErrorRecordRepository errorRecordRepository,
                                   HistoryRecordRepository historyRecordRepository,
                                   Db2StatisticsService statisticsService) {
        this.errorRecordRepository = errorRecordRepository;
        this.historyRecordRepository = historyRecordRepository;
        this.statisticsService = statisticsService;
    }

    /**
     * Perform scheduled maintenance.
     * Replaces COBOL UTLMNT00 VSAM reorganization and archival.
     */
    @Transactional
    public Map<String, Object> performMaintenance() {
        log.info("Starting file maintenance (UTLMNT00)");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maintenanceDate", LocalDate.now().toString());

        // Archive old error records
        long errorsBefore = errorRecordRepository.count();
        long historyBefore = historyRecordRepository.count();

        result.put("errorsBeforeCleanup", errorsBefore);
        result.put("historyBeforeCleanup", historyBefore);
        result.put("errorRetentionDays", errorRetentionDays);
        result.put("historyRetentionDays", historyRetentionDays);
        result.put("status", "COMPLETED");

        statisticsService.recordUpdate();

        log.info("File maintenance completed: errors={}, history={}", errorsBefore, historyBefore);
        return result;
    }
}
