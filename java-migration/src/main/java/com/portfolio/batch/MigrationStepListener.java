package com.portfolio.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Step listener for migration steps
 * Logs step progress and statistics
 */
public class MigrationStepListener implements StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MigrationStepListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("Starting step: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("Completed step: {} - Read: {}, Written: {}, Skipped: {}, Errors: {}",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                stepExecution.getProcessSkipCount() + stepExecution.getReadSkipCount() + stepExecution.getWriteSkipCount());
        
        return stepExecution.getExitStatus();
    }
}
