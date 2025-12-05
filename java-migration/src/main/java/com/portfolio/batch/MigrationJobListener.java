package com.portfolio.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Job listener for migration jobs
 * Logs job start and completion status
 */
public class MigrationJobListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MigrationJobListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Starting migration job: {} at {}", 
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("Completed migration job: {} with status: {} at {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                jobExecution.getEndTime());
        
        if (jobExecution.getStatus().isUnsuccessful()) {
            logger.error("Job failed with exceptions: {}", 
                    jobExecution.getAllFailureExceptions());
        }
    }
}
