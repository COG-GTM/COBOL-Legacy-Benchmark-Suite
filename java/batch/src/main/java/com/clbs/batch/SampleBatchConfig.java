package com.clbs.batch;

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
 * Skeleton Spring Batch wiring to validate the batch tier builds and a job can
 * be defined. Real batch logic (position update, history load, checkpoint/restart)
 * is migrated in later phases.
 */
@Configuration
public class SampleBatchConfig {

    @Bean
    public Tasklet noopTasklet() {
        return (contribution, chunkContext) -> RepeatStatus.FINISHED;
    }

    @Bean
    public Step skeletonStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             Tasklet noopTasklet) {
        return new StepBuilder("skeletonStep", jobRepository)
                .tasklet(noopTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job skeletonJob(JobRepository jobRepository, Step skeletonStep) {
        return new JobBuilder("skeletonJob", jobRepository)
                .start(skeletonStep)
                .build();
    }
}
