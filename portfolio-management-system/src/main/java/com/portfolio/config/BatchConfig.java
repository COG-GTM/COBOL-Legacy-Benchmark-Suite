package com.portfolio.config;

import com.portfolio.batch.HistoryLoadStep;
import com.portfolio.batch.PositionUpdateStep;
import com.portfolio.batch.ReturnCodeStepListener;
import com.portfolio.batch.TransactionValidationStep;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Batch Configuration.
 * Migrated from COBOL batch pipeline:
 *   BCHCTL00 / PRCSEQ00 -> JobLauncher + JobOperator
 *   RCVPRC00 (checkpoint/restart) -> Spring Batch JobRepository
 *   Pipeline: TRNVAL00 -> POSUPD00 -> HISTLD00
 *   RC <= 4 gate between steps (system-architecture.md lines 469-476)
 *   Schedule: daily at 18:00
 */
@Configuration
public class BatchConfig {

    @Value("${portfolio.batch.commit-interval:1000}")
    private int commitInterval;

    @Value("${portfolio.batch.rc-threshold:4}")
    private int rcThreshold;

    @Bean
    public ReturnCodeStepListener returnCodeStepListener() {
        return new ReturnCodeStepListener(rcThreshold);
    }

    @Bean
    public Step txnValidationBatchStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TransactionValidationStep validationStep,
            ReturnCodeStepListener listener) {

        return new StepBuilder("transactionValidationStep", jobRepository)
                .tasklet(validationStep, transactionManager)
                .listener(listener)
                .build();
    }

    @Bean
    public Step posUpdateBatchStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PositionUpdateStep updateStep,
            ReturnCodeStepListener listener) {

        return new StepBuilder("positionUpdateStep", jobRepository)
                .tasklet(updateStep, transactionManager)
                .listener(listener)
                .build();
    }

    @Bean
    public Step histLoadBatchStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            HistoryLoadStep loadStep,
            ReturnCodeStepListener listener) {

        return new StepBuilder("historyLoadStep", jobRepository)
                .tasklet(loadStep, transactionManager)
                .listener(listener)
                .build();
    }

    /**
     * Portfolio Batch Job.
     * Pipeline: TransactionValidation -> PositionUpdate -> HistoryLoad
     * With RC <= 4 gating between each step.
     * RC_0 and RC_4 allow continuation; RC_8 and RC_12 stop the job.
     * (system-architecture.md lines 469-476)
     */
    @Bean
    public Job portfolioBatchJob(
            JobRepository jobRepository,
            Step txnValidationBatchStep,
            Step posUpdateBatchStep,
            Step histLoadBatchStep) {

        return new JobBuilder("portfolioBatchJob", jobRepository)
                .start(txnValidationBatchStep)
                    .on("RC_0").to(posUpdateBatchStep)
                .from(txnValidationBatchStep)
                    .on("RC_4").to(posUpdateBatchStep)
                .from(txnValidationBatchStep)
                    .on("*").fail()
                .from(posUpdateBatchStep)
                    .on("RC_0").to(histLoadBatchStep)
                .from(posUpdateBatchStep)
                    .on("RC_4").to(histLoadBatchStep)
                .from(posUpdateBatchStep)
                    .on("*").fail()
                .from(histLoadBatchStep)
                    .on("*").end()
                .end()
                .build();
    }
}
