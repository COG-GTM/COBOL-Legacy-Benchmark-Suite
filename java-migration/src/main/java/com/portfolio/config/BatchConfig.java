package com.portfolio.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Spring Batch Configuration
 * Configures batch processing infrastructure for data migration
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Value("${portfolio.batch.chunk-size:1000}")
    private int chunkSize;

    @Value("${portfolio.batch.checkpoint-interval:500}")
    private int checkpointInterval;

    @Value("${portfolio.batch.max-retries:3}")
    private int maxRetries;

    /**
     * Configure task executor for parallel batch processing
     */
    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Configure async job launcher for non-blocking job execution
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor("async-batch-"));
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getCheckpointInterval() {
        return checkpointInterval;
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
