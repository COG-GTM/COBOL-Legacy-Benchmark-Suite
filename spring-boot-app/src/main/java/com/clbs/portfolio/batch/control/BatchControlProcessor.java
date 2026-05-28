package com.clbs.portfolio.batch.control;

import com.clbs.portfolio.exception.BatchProcessingException;
import com.clbs.portfolio.common.ReturnCode;
import com.clbs.portfolio.model.BatchControlRecord;
import com.clbs.portfolio.model.BatchControlRecord.BatchControlStatus;
import com.clbs.portfolio.model.BatchControlRecordId;
import com.clbs.portfolio.model.PrerequisiteJob;
import com.clbs.portfolio.repository.BatchControlRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Batch control processor replacing COBOL program BCHCTL00.
 * Manages job-level initialization, prerequisite validation, status tracking, and termination.
 *
 * Paragraph mapping:
 *   1000-PROCESS-INITIALIZE -> initialize()
 *   2000-CHECK-PREREQUISITES -> checkPrerequisites()
 *   3000-UPDATE-STATUS -> updateStatus()
 *   4000-PROCESS-TERMINATE -> terminate()
 */
@Component
public class BatchControlProcessor implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchControlProcessor.class);

    private final BatchControlRecordRepository batchControlRecordRepository;

    public BatchControlProcessor(BatchControlRecordRepository batchControlRecordRepository) {
        this.batchControlRecordRepository = batchControlRecordRepository;
    }

    /**
     * 1000-PROCESS-INITIALIZE: Open files, read control record, validate process, update start status.
     */
    public void initialize(String jobName, String processDate, int sequenceNo) {
        log.info("Initializing batch control for job={}, date={}, seq={}", jobName, processDate, sequenceNo);

        BatchControlRecordId id = new BatchControlRecordId(jobName, processDate, sequenceNo);
        BatchControlRecord record = batchControlRecordRepository.findById(id)
                .orElseThrow(() -> new BatchProcessingException(
                        "Batch control record not found for job: " + jobName,
                        ReturnCode.ERROR, jobName));

        if (record.getStatus() != BatchControlStatus.READY) {
            throw new BatchProcessingException(
                    "Job " + jobName + " is not in READY status (current: " + record.getStatus() + ")",
                    ReturnCode.ERROR, jobName);
        }

        record.setStatus(BatchControlStatus.ACTIVE);
        record.setAttemptTimestamp(LocalDateTime.now());
        batchControlRecordRepository.save(record);

        log.info("Batch control initialized: job={} set to ACTIVE", jobName);
    }

    /**
     * 2000-CHECK-PREREQUISITES: Read control record, check dependencies.
     * Returns true if all prerequisites are satisfied.
     */
    public boolean checkPrerequisites(String jobName, String processDate, int sequenceNo) {
        log.info("Checking prerequisites for job={}", jobName);

        BatchControlRecordId id = new BatchControlRecordId(jobName, processDate, sequenceNo);
        BatchControlRecord record = batchControlRecordRepository.findById(id)
                .orElseThrow(() -> new BatchProcessingException(
                        "Batch control record not found: " + jobName,
                        ReturnCode.ERROR, jobName));

        List<PrerequisiteJob> prereqs = record.getPrereqJobs();
        if (prereqs == null || prereqs.isEmpty()) {
            log.info("No prerequisites for job={}", jobName);
            return true;
        }

        for (PrerequisiteJob prereq : prereqs) {
            List<BatchControlRecord> prereqRecords =
                    batchControlRecordRepository.findByJobNameAndProcessDate(prereq.getPrereqName(), processDate);

            boolean satisfied = prereqRecords.stream()
                    .anyMatch(pr -> pr.getStatus() == BatchControlStatus.DONE
                            && (prereq.getPrereqRc() == null || pr.getReturnCode() <= prereq.getPrereqRc()));

            if (!satisfied) {
                log.warn("Prerequisite not satisfied: {} for job={}", prereq.getPrereqName(), jobName);
                record.setStatus(BatchControlStatus.WAITING);
                batchControlRecordRepository.save(record);
                return false;
            }
        }

        log.info("All prerequisites satisfied for job={}", jobName);
        return true;
    }

    /**
     * 3000-UPDATE-STATUS: Read control record, update process status, write control record.
     */
    public void updateStatus(String jobName, String processDate, int sequenceNo,
                             BatchControlStatus newStatus, Integer returnCode, String errorDesc) {
        log.info("Updating status for job={} to {}", jobName, newStatus);

        BatchControlRecordId id = new BatchControlRecordId(jobName, processDate, sequenceNo);
        BatchControlRecord record = batchControlRecordRepository.findById(id)
                .orElseThrow(() -> new BatchProcessingException(
                        "Batch control record not found: " + jobName,
                        ReturnCode.ERROR, jobName));

        record.setStatus(newStatus);
        if (returnCode != null) {
            record.setReturnCode(returnCode);
        }
        if (errorDesc != null) {
            record.setErrorDesc(errorDesc);
        }
        batchControlRecordRepository.save(record);
    }

    /**
     * 4000-PROCESS-TERMINATE: Update completion, close files.
     */
    public void terminate(String jobName, String processDate, int sequenceNo, int returnCode) {
        log.info("Terminating batch control for job={}, rc={}", jobName, returnCode);

        BatchControlRecordId id = new BatchControlRecordId(jobName, processDate, sequenceNo);
        BatchControlRecord record = batchControlRecordRepository.findById(id)
                .orElseThrow(() -> new BatchProcessingException(
                        "Batch control record not found: " + jobName,
                        ReturnCode.ERROR, jobName));

        record.setStatus(returnCode <= ReturnCode.WARNING.getCode()
                ? BatchControlStatus.DONE : BatchControlStatus.ERROR);
        record.setReturnCode(returnCode);
        record.setCompleteTimestamp(LocalDateTime.now());
        if (record.getStartTime() != null) {
            record.setEndTime(LocalDateTime.now().toLocalTime().toString().substring(0, 8));
        }
        batchControlRecordRepository.save(record);

        log.info("Batch control terminated: job={}, status={}", jobName, record.getStatus());
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        log.info("Spring Batch job starting: {}", jobName);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        log.info("Spring Batch job completed: {} with status {}", jobName, jobExecution.getStatus());
    }
}
