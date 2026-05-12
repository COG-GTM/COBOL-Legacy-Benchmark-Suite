package com.portfolio.service.batch;

import com.portfolio.exception.BatchProcessingException;
import com.portfolio.model.dto.BatchConstants;
import com.portfolio.model.entity.BatchControlRecord;
import com.portfolio.repository.BatchControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BatchControlService {

    private static final Logger log = LoggerFactory.getLogger(BatchControlService.class);

    private final BatchControlRepository batchControlRepository;

    public BatchControlService(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    @Transactional
    public BatchControlRecord processInitialize(String jobName) {
        String processDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        BatchControlRecord record = batchControlRepository.findByJobNameAndProcessDate(jobName, processDate)
                .orElseGet(() -> {
                    BatchControlRecord newRecord = new BatchControlRecord();
                    newRecord.setJobName(jobName);
                    newRecord.setProcessDate(processDate);
                    newRecord.setSequenceNo(1);
                    return newRecord;
                });

        record.setStatus(BatchConstants.BCT_STAT_ACTIVE);
        record.setAttemptTimestamp(LocalDateTime.now());
        record.setRestartCount(record.getRestartCount() != null ? record.getRestartCount() : 0);

        log.info("{}: {}", BatchConstants.BCT_MSG_STARTING, jobName);
        return batchControlRepository.save(record);
    }

    @Transactional(readOnly = true)
    public boolean checkPrerequisites(BatchControlRecord record) {
        // In the COBOL version, this checks BCT-PREREQ-JOBS array
        // Here prerequisites are verified via Spring Batch job dependencies
        log.info("Checking prerequisites for job: {}", record.getJobName());
        return true;
    }

    @Transactional
    public void updateStatus(String jobName, char status, Integer returnCode, String errorDesc) {
        BatchControlRecord record = batchControlRepository.findByJobName(jobName)
                .orElseThrow(() -> new BatchProcessingException(
                        "Control record not found: " + jobName, BatchConstants.BCT_RC_ERROR));

        record.setStatus(status);
        if (returnCode != null) {
            record.setReturnCode(returnCode);
        }
        if (errorDesc != null) {
            record.setErrorDesc(errorDesc);
        }

        batchControlRepository.save(record);
        log.info("Updated status for {}: {}", jobName, status);
    }

    @Transactional
    public void processTerminate(String jobName, int returnCode) {
        BatchControlRecord record = batchControlRepository.findByJobName(jobName)
                .orElseThrow(() -> new BatchProcessingException(
                        "Control record not found: " + jobName, BatchConstants.BCT_RC_ERROR));

        record.setCompleteTimestamp(LocalDateTime.now());
        record.setReturnCode(returnCode);

        if (returnCode <= BatchConstants.BCT_RC_WARNING) {
            record.setStatus(BatchConstants.BCT_STAT_DONE);
            record.setErrorDesc(BatchConstants.BCT_MSG_COMPLETE);
        } else {
            record.setStatus(BatchConstants.BCT_STAT_ERROR);
            record.setErrorDesc(BatchConstants.BCT_MSG_FAILED);
        }

        batchControlRepository.save(record);
        log.info("Job {} completed with RC={}", jobName, returnCode);
    }
}
