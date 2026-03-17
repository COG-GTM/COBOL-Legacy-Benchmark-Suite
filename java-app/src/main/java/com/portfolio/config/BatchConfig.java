package com.portfolio.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Spring Batch infrastructure configuration.
 * Replaces: JCL batch orchestration and BCHCTL00.cbl batch control infrastructure.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    /**
     * Asynchronous job launcher for running batch jobs without blocking.
     * Replaces the JCL job submission mechanism.
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor("batch-"));
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}
