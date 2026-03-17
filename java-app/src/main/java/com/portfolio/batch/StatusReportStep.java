package com.portfolio.batch;

import com.portfolio.reporting.StatusReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Status Report Step - Tasklet.
 * Replaces: RPTSTA00.cbl as a Spring Batch step.
 * Generates system performance and statistics reports.
 */
@Component
public class StatusReportStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(StatusReportStep.class);

    private final StatusReportGenerator reportGenerator;

    public StatusReportStep(StatusReportGenerator reportGenerator) {
        this.reportGenerator = reportGenerator;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Starting status report generation");
        String report = reportGenerator.generateReport();
        log.info("Status report generated: {} characters", report.length());
        return RepeatStatus.FINISHED;
    }
}
