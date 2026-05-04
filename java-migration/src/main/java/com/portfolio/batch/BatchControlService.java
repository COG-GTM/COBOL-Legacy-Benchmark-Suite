package com.portfolio.batch;

import com.portfolio.domain.BatchConstants;
import com.portfolio.domain.BatchControlRecord;
import com.portfolio.repository.BatchControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Batch Control Service - migrated from COBOL BCHCTL00.cbl.
 * Job-level control, status tracking, dependency checking.
 * BCT-STATUS-READY/ACTIVE/WAITING/DONE/ERROR -> Spring Batch BatchStatus.
 */
@Service
public class BatchControlService {

    private static final Logger log = LoggerFactory.getLogger(BatchControlService.class);

    private final BatchControlRepository batchControlRepository;

    public BatchControlService(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    @Transactional
    public BatchControlRecord initializeJob(String jobName, int sequenceNo) {
        BatchControlRecord record = new BatchControlRecord();
        record.setJobName(jobName);
        record.setProcessDate(LocalDate.now());
        record.setSequenceNo(sequenceNo);
        record.setStatus(BatchConstants.STATUS_READY);
        record.setAttemptTs(LocalDateTime.now());

        log.info("Initializing batch job: {} seq: {}", jobName, sequenceNo);
        return batchControlRepository.save(record);
    }

    @Transactional
    public void markActive(BatchControlRecord record) {
        record.setStatus(BatchConstants.STATUS_ACTIVE);
        record.setAttemptTs(LocalDateTime.now());
        batchControlRepository.save(record);
        log.info("Job {} marked ACTIVE", record.getJobName());
    }

    @Transactional
    public void markComplete(BatchControlRecord record, int returnCode) {
        record.setStatus(BatchConstants.STATUS_DONE);
        record.setReturnCode(returnCode);
        record.setCompleteTs(LocalDateTime.now());
        batchControlRepository.save(record);
        log.info("Job {} completed with RC={}", record.getJobName(), returnCode);
    }

    @Transactional
    public void markError(BatchControlRecord record, int returnCode, String errorDesc) {
        record.setStatus(BatchConstants.STATUS_ERROR);
        record.setReturnCode(returnCode);
        record.setErrorDesc(errorDesc);
        record.setCompleteTs(LocalDateTime.now());
        batchControlRepository.save(record);
        log.error("Job {} failed: RC={} - {}", record.getJobName(), returnCode, errorDesc);
    }

    public boolean checkPrerequisites(String jobName, LocalDate processDate) {
        List<BatchControlRecord> prereqs = batchControlRepository
                .findByJobNameAndProcessDate(jobName, processDate);

        return prereqs.stream()
                .allMatch(r -> BatchConstants.STATUS_DONE.equals(r.getStatus())
                        && r.getReturnCode() <= BatchConstants.RC_WARNING);
    }
}
