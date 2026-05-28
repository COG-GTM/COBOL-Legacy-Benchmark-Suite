package com.clbs.portfolio.batch;

import com.clbs.portfolio.service.report.PositionReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component("positionReportTasklet")
public class PositionReportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(PositionReportTasklet.class);

    private final PositionReportService positionReportService;

    public PositionReportTasklet(PositionReportService positionReportService) {
        this.positionReportService = positionReportService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Executing position report batch job");
        positionReportService.generateReport(LocalDate.now(), "text");
        positionReportService.generateReport(LocalDate.now(), "csv");
        return RepeatStatus.FINISHED;
    }
}
