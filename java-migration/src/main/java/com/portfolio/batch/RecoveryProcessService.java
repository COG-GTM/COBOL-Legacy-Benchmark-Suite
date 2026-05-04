package com.portfolio.batch;

import com.portfolio.domain.BatchConstants;
import com.portfolio.domain.BatchControlRecord;
import com.portfolio.repository.BatchControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Recovery Process Service - migrated from COBOL RCVPRC00.cbl.
 * Handles batch job recovery and restart logic.
 */
@Service
public class RecoveryProcessService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryProcessService.class);

    private final BatchControlRepository batchControlRepository;

    public RecoveryProcessService(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    @Transactional
    public boolean recoverJob(String jobName) {
        List<BatchControlRecord> failedJobs = batchControlRepository
                .findByJobNameAndProcessDate(jobName, LocalDate.now());

        for (BatchControlRecord record : failedJobs) {
            if (BatchConstants.STATUS_ERROR.equals(record.getStatus())) {
                if (record.getRestartCount() < BatchConstants.MAX_RESTARTS) {
                    record.setStatus(BatchConstants.STATUS_READY);
                    record.setRestartCount(record.getRestartCount() + 1);
                    batchControlRepository.save(record);
                    log.info("Job {} recovered, restart count: {}",
                            jobName, record.getRestartCount());
                    return true;
                } else {
                    log.error("Job {} exceeded max restarts ({})",
                            jobName, BatchConstants.MAX_RESTARTS);
                    return false;
                }
            }
        }

        log.info("No failed jobs found for recovery: {}", jobName);
        return false;
    }
}
