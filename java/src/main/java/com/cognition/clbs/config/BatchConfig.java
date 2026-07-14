package com.cognition.clbs.config;

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
 * Spring Batch wiring for the migrated batch programs (TRNVAL00, POSUPD00,
 * HISTLD00, reporting jobs, ...).
 *
 * <p>The single-step {@code sampleJob} below is a scaffold placeholder that
 * proves the batch infrastructure (JobRepository, transaction manager, job
 * launcher) is wired correctly. Later phases replace it with the real jobs.
 * Automatic execution on startup is disabled via {@code spring.batch.job.enabled=false}.
 */
@Configuration
public class BatchConfig {

    @Bean
    public Step sampleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("sampleStep", jobRepository)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
                .build();
    }

    @Bean
    public Job sampleJob(JobRepository jobRepository, Step sampleStep) {
        return new JobBuilder("sampleJob", jobRepository)
                .start(sampleStep)
                .build();
    }
}
