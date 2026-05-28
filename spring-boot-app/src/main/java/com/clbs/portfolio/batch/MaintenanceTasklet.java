package com.clbs.portfolio.batch;

import com.clbs.portfolio.service.maintenance.MaintenanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("maintenanceTasklet")
public class MaintenanceTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceTasklet.class);

    private final MaintenanceService maintenanceService;

    public MaintenanceTasklet(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Executing maintenance batch job");
        maintenanceService.executeMaintenance(
                List.of("ARCHIVE", "CLEANUP", "REORG", "ANALYZE"));
        return RepeatStatus.FINISHED;
    }
}
