package com.cog.gtm.clbs.migration.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Sample Spring Batch configuration that demonstrates a simple tasklet job.
 *
 * <p>Batch jobs are not launched automatically on startup; they can be run by
 * {@code spring-boot:run} with {@code spring.batch.job.enabled=true} or by
 * a later command-line runner.
 */
@Configuration
public class ValidationBatchConfig {

    @Bean
    public Job portfolioValidationJob(JobRepository jobRepository, Step validationStep) {
        return new JobBuilder("portfolioValidationJob", jobRepository)
                .start(validationStep)
                .build();
    }

    @Bean
    public Step validationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            // Placeholder for a real step; in later phases this will read
            // transactions, validate them, and write positions/history.
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("validationStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}
