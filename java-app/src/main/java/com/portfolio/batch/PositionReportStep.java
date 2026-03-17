package com.portfolio.batch;

import com.portfolio.reporting.PositionReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Position Report Step - Tasklet.
 * Replaces: RPTPOS00.cbl as a Spring Batch step.
 * Generates portfolio valuation and summary reports.
 */
@Component
public class PositionReportStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(PositionReportStep.class);

    private final PositionReportGenerator reportGenerator;

    public PositionReportStep(PositionReportGenerator reportGenerator) {
        this.reportGenerator = reportGenerator;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Starting position report generation");
        String report = reportGenerator.generateReport();
        log.info("Position report generated: {} characters", report.length());
        return RepeatStatus.FINISHED;
    }
}
