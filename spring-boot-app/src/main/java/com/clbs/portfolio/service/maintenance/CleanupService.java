package com.clbs.portfolio.service.maintenance;

import com.clbs.portfolio.entity.BatchControlRecord;
import com.clbs.portfolio.entity.CheckpointControl;
import com.clbs.portfolio.entity.ErrorLog;
import com.clbs.portfolio.enums.BatchStatus;
import com.clbs.portfolio.repository.BatchControlRecordRepository;
import com.clbs.portfolio.repository.CheckpointControlRepository;
import com.clbs.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CleanupService {

    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    private final CheckpointControlRepository checkpointControlRepository;
    private final ErrorLogRepository errorLogRepository;
    private final BatchControlRecordRepository batchControlRecordRepository;

    @Value("${maintenance.cleanup.checkpoint-retention-days:30}")
    private int checkpointRetentionDays;

    @Value("${maintenance.cleanup.error-log-retention-days:90}")
    private int errorLogRetentionDays;

    public CleanupService(CheckpointControlRepository checkpointControlRepository,
                           ErrorLogRepository errorLogRepository,
                           BatchControlRecordRepository batchControlRecordRepository) {
        this.checkpointControlRepository = checkpointControlRepository;
        this.errorLogRepository = errorLogRepository;
        this.batchControlRecordRepository = batchControlRecordRepository;
    }

    @Transactional
    public MaintenanceResult cleanup() {
        MaintenanceResult result = new MaintenanceResult("CLEANUP");

        // Clean up expired checkpoint records
        LocalDateTime checkpointCutoff = LocalDateTime.now().minusDays(checkpointRetentionDays);
        List<CheckpointControl> expiredCheckpoints = checkpointControlRepository.findExpired(checkpointCutoff);
        result.setRecordsProcessed(result.getRecordsProcessed() + expiredCheckpoints.size());
        checkpointControlRepository.deleteAll(expiredCheckpoints);
        result.addDetail(String.format("Removed %d expired checkpoint records", expiredCheckpoints.size()));

        // Clean up old error logs
        LocalDateTime errorLogCutoff = LocalDateTime.now().minusDays(errorLogRetentionDays);
        List<ErrorLog> oldErrorLogs = errorLogRepository.findOlderThan(errorLogCutoff);
        result.setRecordsProcessed(result.getRecordsProcessed() + oldErrorLogs.size());
        errorLogRepository.deleteAll(oldErrorLogs);
        result.addDetail(String.format("Removed %d old error log records", oldErrorLogs.size()));

        // Clean up completed batch control records
        List<BatchControlRecord> completedBatch = batchControlRecordRepository.findByStatus(BatchStatus.DONE);
        result.setRecordsProcessed(result.getRecordsProcessed() + completedBatch.size());
        batchControlRecordRepository.deleteAll(completedBatch);
        result.addDetail(String.format("Removed %d completed batch control records", completedBatch.size()));

        long totalCleaned = expiredCheckpoints.size() + oldErrorLogs.size() + completedBatch.size();
        result.setRecordsAffected(totalCleaned);
        log.info("Cleanup complete: {} records removed", totalCleaned);
        return result;
    }
}
