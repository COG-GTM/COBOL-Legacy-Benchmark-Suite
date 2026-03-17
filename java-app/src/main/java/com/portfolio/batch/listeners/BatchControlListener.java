package com.portfolio.batch.listeners;

import com.portfolio.model.BatchControlKey;
import com.portfolio.model.BatchControlRecord;
import com.portfolio.model.enums.BatchStatus;
import com.portfolio.repository.BatchControlRepository;
import com.portfolio.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Batch Control Listener.
 * Implements JobExecutionListener to update batch control records
 * on job start/completion/failure.
 * Replaces: COBOL return code propagation pattern from BCHCTL00.cbl.
 */
@Component
public class BatchControlListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchControlListener.class);

    private final BatchControlRepository batchControlRepository;
    private final AuditService auditService;

    public BatchControlListener(BatchControlRepository batchControlRepository,
                                 AuditService auditService) {
        this.batchControlRepository = batchControlRepository;
        this.auditService = auditService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        log.info("Job starting: {}", jobName);

        auditService.logSystemEvent("STARTUP", "SUCC",
                "Job started: " + jobName);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        // Use the job's start time for processDate to match the INIT step,
        // avoiding mismatches if the job spans midnight
        LocalDateTime startTime = jobExecution.getStartTime() != null
                ? jobExecution.getStartTime() : LocalDateTime.now();
        String processDate = startTime
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        Optional<BatchControlRecord> recordOpt = batchControlRepository
                .findById(new BatchControlKey(jobName, processDate, 1));

        recordOpt.ifPresent(record -> {
            if (jobExecution.getStatus().isUnsuccessful()) {
                record.setStatus(BatchStatus.ERROR.getCode());
                record.setReturnCode(8);
                record.setErrorDesc("Job failed: " + jobExecution.getExitStatus().getExitDescription());
            } else {
                record.setStatus(BatchStatus.DONE.getCode());
                record.setReturnCode(0);
            }
            record.setEndTime(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            record.setCompleteTs(LocalDateTime.now());
            batchControlRepository.save(record);
        });

        String status = jobExecution.getStatus().isUnsuccessful() ? "FAIL" : "SUCC";
        auditService.logSystemEvent("SHUTDOWN", status,
                "Job completed: " + jobName + " status=" + jobExecution.getStatus());

        log.info("Job completed: {} with status {}", jobName, jobExecution.getStatus());
    }
}
