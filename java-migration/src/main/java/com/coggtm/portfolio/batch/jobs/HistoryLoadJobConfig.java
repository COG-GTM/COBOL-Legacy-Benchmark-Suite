package com.coggtm.portfolio.batch.jobs;

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
 * Batch job: History Load.
 * Maps to COBOL program HISTLD00 and JCL job HISTLDJB.
 *
 * <p>Step: read positions → load to POSHIST table.
 * Chunk size 1000 matches COBOL WS-COMMIT-THRESHOLD.</p>
 */
@Configuration
public class HistoryLoadJobConfig {

    private static final int COMMIT_THRESHOLD = 1000;

    @Bean
    public Job historyLoadJob(JobRepository jobRepository, Step historyLoadStep) {
        // TODO: Wire ItemReader, ItemProcessor, and ItemWriter from HISTLD00
        return new JobBuilder("historyLoadJob", jobRepository)
                .start(historyLoadStep)
                .build();
    }

    @Bean
    public Step historyLoadStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        // TODO: Replace tasklet with chunk-oriented step (chunk size = COMMIT_THRESHOLD)
        return new StepBuilder("historyLoadStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // Placeholder — HISTLD00 history load logic goes here
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
