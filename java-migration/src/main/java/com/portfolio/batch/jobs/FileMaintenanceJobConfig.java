package com.portfolio.batch.jobs;

import com.portfolio.repository.ErrorLogRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
public class FileMaintenanceJobConfig {

    private static final Logger log = LoggerFactory.getLogger(FileMaintenanceJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ErrorLogRepository errorLogRepository;
    private final JdbcTemplate jdbcTemplate;

    public FileMaintenanceJobConfig(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    ErrorLogRepository errorLogRepository,
                                    JdbcTemplate jdbcTemplate) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.errorLogRepository = errorLogRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Bean
    public Job fileMaintenanceJob() {
        return new JobBuilder("fileMaintenanceJob", jobRepository)
                .start(archiveStep())
                .next(cleanupStep())
                .next(analyzeStep())
                .build();
    }

    @Bean
    public Step archiveStep() {
        return new StepBuilder("archiveStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("ARCHIVE: Archiving old records...");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate cutoff = LocalDate.now().minusDays(90);
                    int deleted = errorLogRepository.deleteOlderThan(cutoff);
                    log.info("CLEANUP: Deleted {} old error log entries before {}", deleted, cutoff);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step analyzeStep() {
        return new StepBuilder("analyzeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    long portfolioCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM portfolio_master", Long.class);
                    long transactionCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM transaction_history", Long.class);
                    log.info("ANALYZE: Portfolios={}, Transactions={}", portfolioCount, transactionCount);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
