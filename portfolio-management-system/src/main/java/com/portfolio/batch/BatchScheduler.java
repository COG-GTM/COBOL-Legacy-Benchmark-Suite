package com.portfolio.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Batch Scheduler.
 * Migrated from COBOL BCHCTL00/PRCSEQ00 + JCL scheduling.
 * Replaces z/OS job scheduler with Spring @Scheduled.
 * Runs daily at 18:00 (from system-architecture.md lines 469-476).
 */
@Component
public class BatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job portfolioBatchJob;

    public BatchScheduler(JobLauncher jobLauncher, Job portfolioBatchJob) {
        this.jobLauncher = jobLauncher;
        this.portfolioBatchJob = portfolioBatchJob;
    }

    /**
     * Execute the daily batch pipeline.
     * Schedule: daily at 18:00 (cron from application.properties).
     * Pipeline: TRNVAL00 -> POSUPD00 -> HISTLD00
     */
    @Scheduled(cron = "${portfolio.batch.schedule.cron}")
    public void executeDailyBatch() {
        log.info("Starting daily batch pipeline at {}", LocalDateTime.now());

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runDate", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(portfolioBatchJob, params);
            log.info("Daily batch pipeline completed successfully");
        } catch (Exception e) {
            log.error("Daily batch pipeline failed: {}", e.getMessage(), e);
        }
    }
}
