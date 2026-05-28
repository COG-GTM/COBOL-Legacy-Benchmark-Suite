package com.clbs.portfolio.config;

import com.clbs.portfolio.batch.control.BatchControlProcessor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration.
 * Configures JobRepository, JobLauncher, and transaction management.
 * Chunk size defaults to 1000, matching COBOL CK-COMMIT-FREQ from CKPRST.cpy.
 */
@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    /** Default chunk/commit interval matching COBOL CK-COMMIT-FREQ */
    public static final int DEFAULT_CHUNK_SIZE = 1000;

    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
