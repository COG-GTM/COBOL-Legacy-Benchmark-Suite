package com.clbs.portfolio.batch;

import com.clbs.portfolio.service.report.SystemStatsReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component("systemStatsReportTasklet")
public class SystemStatsReportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(SystemStatsReportTasklet.class);

    private final SystemStatsReportService systemStatsReportService;

    public SystemStatsReportTasklet(SystemStatsReportService systemStatsReportService) {
        this.systemStatsReportService = systemStatsReportService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Executing system stats report batch job");
        LocalDate today = LocalDate.now();
        systemStatsReportService.generateReport(today.minusDays(1), today);
        return RepeatStatus.FINISHED;
    }
}
