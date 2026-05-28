package com.clbs.portfolio.batch;

import com.clbs.portfolio.service.report.AuditReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component("auditReportTasklet")
public class AuditReportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(AuditReportTasklet.class);

    private final AuditReportService auditReportService;

    public AuditReportTasklet(AuditReportService auditReportService) {
        this.auditReportService = auditReportService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Executing audit report batch job");
        LocalDate today = LocalDate.now();
        auditReportService.generateReport(today.minusDays(1), today, "text");
        auditReportService.generateReport(today.minusDays(1), today, "csv");
        return RepeatStatus.FINISHED;
    }
}
