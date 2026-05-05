package com.portfolio.portmstr.batch.listener;

import com.portfolio.portmstr.batch.PortfolioBatchProcessor;
import com.portfolio.portmstr.batch.checkpoint.CheckpointService;
import com.portfolio.portmstr.service.ErrorLoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Batch job lifecycle listener.
 * Replaces COBOL batch JCL STEP processing with before/after hooks.
 *
 * Equivalent to:
 *   Before: COBOL 1000-INITIALIZE paragraph (open files, initialize counters)
 *   After:  COBOL 6000-TERMINATE paragraph (close files, write control totals)
 */
@Component
public class BatchJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchJobListener.class);
    private static final String PROGRAM_ID = "PORTBAT";

    private final CheckpointService checkpointService;
    private final ErrorLoggingService errorLoggingService;
    private final PortfolioBatchProcessor batchProcessor;

    public BatchJobListener(CheckpointService checkpointService,
                            ErrorLoggingService errorLoggingService,
                            PortfolioBatchProcessor batchProcessor) {
        this.checkpointService = checkpointService;
        this.errorLoggingService = errorLoggingService;
        this.batchProcessor = batchProcessor;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting batch job: {}", jobExecution.getJobInstance().getJobName());
        batchProcessor.resetCounters();
        checkpointService.initializeCheckpoint(PROGRAM_ID);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            checkpointService.completeCheckpoint(PROGRAM_ID);
            log.info("Batch job completed successfully: {}",
                    jobExecution.getJobInstance().getJobName());
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            checkpointService.failCheckpoint(PROGRAM_ID,
                    "Job failed: " + jobExecution.getAllFailureExceptions());

            errorLoggingService.logError(
                    PROGRAM_ID,
                    'S',
                    12,
                    "JOB-FAIL",
                    "Batch job failed",
                    "SYSTEM",
                    jobExecution.getAllFailureExceptions().toString()
            );
        }
    }
}
