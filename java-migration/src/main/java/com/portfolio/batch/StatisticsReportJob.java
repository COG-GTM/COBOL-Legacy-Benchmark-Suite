package com.portfolio.batch;

import com.portfolio.service.common.DatabaseStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Statistics Report Job - migrated from COBOL RPTSTA00.cbl.
 * System performance metrics and trend analysis.
 */
@Configuration
public class StatisticsReportJob {

    private static final Logger log = LoggerFactory.getLogger(StatisticsReportJob.class);

    private final DatabaseStatisticsService databaseStatisticsService;

    public StatisticsReportJob(DatabaseStatisticsService databaseStatisticsService) {
        this.databaseStatisticsService = databaseStatisticsService;
    }

    @Bean
    public Job statisticsReportBatchJob(JobRepository jobRepository, Step statisticsReportStep) {
        return new JobBuilder("statisticsReportJob", jobRepository)
                .start(statisticsReportStep)
                .build();
    }

    @Bean
    public Step statisticsReportStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager) {
        return new StepBuilder("statisticsReportStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    databaseStatisticsService.displayStatistics();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
