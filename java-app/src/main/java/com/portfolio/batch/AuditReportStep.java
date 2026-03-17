package com.portfolio.batch;

import com.portfolio.reporting.AuditReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Audit Report Step - Tasklet.
 * Replaces: RPTAUD00.cbl as a Spring Batch step.
 * Generates security and process audit trail reports.
 */
@Component
public class AuditReportStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(AuditReportStep.class);

    private final AuditReportGenerator reportGenerator;

    public AuditReportStep(AuditReportGenerator reportGenerator) {
        this.reportGenerator = reportGenerator;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Starting audit report generation");
        String report = reportGenerator.generateReport();
        log.info("Audit report generated: {} characters", report.length());
        return RepeatStatus.FINISHED;
    }
}
