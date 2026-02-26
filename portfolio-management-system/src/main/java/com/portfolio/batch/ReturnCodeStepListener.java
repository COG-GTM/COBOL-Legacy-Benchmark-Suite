package com.portfolio.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Return Code Step Listener.
 * Implements the RC <= 4 gate logic from COBOL batch pipeline.
 * (system-architecture.md lines 469-476)
 * If a step exits with RC > threshold, the job stops.
 */
public class ReturnCodeStepListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ReturnCodeStepListener.class);

    private final int rcThreshold;

    public ReturnCodeStepListener(int rcThreshold) {
        this.rcThreshold = rcThreshold;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting step: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        ExitStatus exitStatus = stepExecution.getExitStatus();

        // Extract return code from exit status
        int returnCode = extractReturnCode(exitStatus);

        log.info("Step {} completed with RC={}", stepName, returnCode);

        if (returnCode > rcThreshold) {
            log.error("Step {} RC={} exceeds threshold {}. Job will stop.",
                    stepName, returnCode, rcThreshold);
            return new ExitStatus("RC_" + returnCode);
        }

        if (returnCode > 0) {
            log.warn("Step {} completed with warning RC={}", stepName, returnCode);
            return new ExitStatus("RC_" + returnCode);
        }

        return new ExitStatus("RC_0");
    }

    private int extractReturnCode(ExitStatus exitStatus) {
        String exitCode = exitStatus.getExitCode();
        if (exitCode.startsWith("RC_")) {
            try {
                return Integer.parseInt(exitCode.substring(3));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if ("COMPLETED".equals(exitCode)) return 0;
        if ("FAILED".equals(exitCode)) return 8;
        return 0;
    }
}
