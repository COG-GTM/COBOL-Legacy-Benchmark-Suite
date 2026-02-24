package com.portfolio.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration - replaces COBOL batch JCL pipeline.
 * Source: documentation/technical/system-architecture.md batch processing flow
 *
 * COBOL pipeline: Start of Day → TRNVAL00 (RC≤4) → POSUPD00 (RC≤4) → HISTLD00 (RC≤4) → Reports → End of Day
 * Spring Batch: endOfDayJob with sequential steps using flow decisions
 */
@Configuration
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionValidationStep transactionValidationStep;
    private final PositionUpdateStep positionUpdateStep;
    private final HistoryLoadStep historyLoadStep;

    public BatchConfig(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager,
                       TransactionValidationStep transactionValidationStep,
                       PositionUpdateStep positionUpdateStep,
                       HistoryLoadStep historyLoadStep) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.transactionValidationStep = transactionValidationStep;
        this.positionUpdateStep = positionUpdateStep;
        this.historyLoadStep = historyLoadStep;
    }

    /**
     * End-of-day processing job - replaces the full COBOL batch pipeline.
     * Steps execute sequentially with RC checking via step listeners.
     */
    @Bean
    public Job endOfDayJob() {
        return new JobBuilder("endOfDayJob", jobRepository)
                .start(transactionValidationStepDef())
                .next(positionUpdateStepDef())
                .next(historyLoadStepDef())
                .build();
    }

    /**
     * Step 1: TRNVAL00 - Transaction Validation.
     * Reads pending transactions, validates fields, marks status.
     */
    @Bean
    public Step transactionValidationStepDef() {
        return new StepBuilder("transactionValidation", jobRepository)
                .tasklet(transactionValidationStep, transactionManager)
                .build();
    }

    /**
     * Step 2: POSUPD00 - Position Update.
     * Updates investment positions based on validated transactions.
     */
    @Bean
    public Step positionUpdateStepDef() {
        return new StepBuilder("positionUpdate", jobRepository)
                .tasklet(positionUpdateStep, transactionManager)
                .build();
    }

    /**
     * Step 3: HISTLD00 - History Load.
     * Loads processed transactions into position history.
     */
    @Bean
    public Step historyLoadStepDef() {
        return new StepBuilder("historyLoad", jobRepository)
                .tasklet(historyLoadStep, transactionManager)
                .build();
    }

    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        launcher.afterPropertiesSet();
        return launcher;
    }
}
