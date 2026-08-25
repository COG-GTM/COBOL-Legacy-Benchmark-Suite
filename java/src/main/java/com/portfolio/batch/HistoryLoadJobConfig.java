package com.portfolio.batch;

import com.portfolio.domain.PositionHistory;
import com.portfolio.domain.TransactionHistoryFileRecord;
import com.portfolio.repository.TransactionHistoryFileRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.batch.core.SkipListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import com.portfolio.common.ErrorHandlingService;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Batch migration of {@code src/programs/batch/HISTLD00.cbl}
 * (Position History DB2 Load) — the reference vertical slice.
 *
 * <p>COBOL → Spring Batch mapping:
 * <ul>
 *   <li>Sequential READ of the indexed TRANSACTION-HISTORY file (2100) →
 *       {@link RepositoryItemReader} over the VSAM_TRANHIST table, ordered by
 *       the COBOL RECORD KEY</li>
 *   <li>2200-LOAD-TO-DB2 field mapping/validation → {@link HistoryItemProcessor}</li>
 *   <li>INSERT INTO POSHIST + SQLCODE -803 handling → {@link HistoryItemWriter}</li>
 *   <li>2300-CHECK-COMMIT with WS-COMMIT-THRESHOLD 1000 → chunk size 1000
 *       (each chunk boundary is a commit)</li>
 *   <li>2310-UPDATE-CHECKPOINT (REWRITE of the BCHCTL record) → afterChunk
 *       listener updating the batch control table</li>
 *   <li>WS-ERROR-COUNT > 100 abort → {@link ErrorLimitExceededException}
 *       thrown from the processor</li>
 *   <li>MOVE WS-ERROR-COUNT TO RETURN-CODE → process exit code from
 *       {@link HistoryLoadJobRunner}</li>
 * </ul>
 */
@Configuration
public class HistoryLoadJobConfig {

    /** WS-COMMIT-THRESHOLD PIC S9(4) COMP VALUE 1000. */
    public static final int COMMIT_THRESHOLD = 1000;

    public static final String JOB_NAME = "histld00Job";
    public static final String PROGRAM_ID = "HISTLD00";

    @Bean
    @StepScope
    public RepositoryItemReader<TransactionHistoryFileRecord> historyItemReader(
            TransactionHistoryFileRepository repository) {
        Map<String, Sort.Direction> sorts = new LinkedHashMap<>();
        sorts.put("key.transDate", Sort.Direction.ASC);
        sorts.put("key.transTime", Sort.Direction.ASC);
        sorts.put("key.portfolioId", Sort.Direction.ASC);
        sorts.put("key.sequenceNo", Sort.Direction.ASC);
        return new RepositoryItemReaderBuilder<TransactionHistoryFileRecord>()
                .name("historyItemReader")
                .repository(repository)
                .methodName("findAll")
                .pageSize(COMMIT_THRESHOLD)
                .sorts(sorts)
                .build();
    }

    @Bean
    public Step histld00Step(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             RepositoryItemReader<TransactionHistoryFileRecord> historyItemReader,
                             HistoryItemProcessor processor,
                             HistoryItemWriter writer,
                             BatchControlService batchControlService,
                             HistoryLoadStats stats,
                             ErrorHandlingService errorHandlingService) {
        return new StepBuilder("histld00Step", jobRepository)
                .<TransactionHistoryFileRecord, PositionHistory>chunk(COMMIT_THRESHOLD, transactionManager)
                .reader(historyItemReader)
                .processor(processor)
                .writer(writer)
                // COBOL 2200-LOAD-TO-DB2: an INSERT failure other than SQLCODE
                // -803 increments WS-ERROR-COUNT and processing continues until
                // the count exceeds 100 (WS-ERROR-COUNT > 100 abort).
                .faultTolerant()
                // Required: the processor counts reads/errors, so it must not
                // be re-run during the fault-tolerant chunk scan.
                .processorNonTransactional()
                // skipCount is Spring Batch's live write-skip counter, so the
                // WS-ERROR-COUNT > 100 limit combines validation errors (in
                // stats) with insert failures as they happen. During the run,
                // stats.errorCount holds validation errors only; write skips
                // are folded in once in afterStep below.
                .skipPolicy((throwable, skipCount) ->
                        throwable instanceof DataAccessException
                                && stats.getErrorCount() + skipCount < HistoryLoadStats.MAX_ERRORS)
                .listener(new SkipListener<TransactionHistoryFileRecord, PositionHistory>() {
                    @Override
                    public void onSkipInWrite(PositionHistory item, Throwable t) {
                        errorHandlingService.logError(PROGRAM_ID, "S", 3, "HIST0002",
                                "POSHIST insert failed: " + t.getMessage(),
                                String.valueOf(item.getKey().getAccountNo() + "/"
                                        + item.getKey().getPortfolioId()));
                    }
                })
                .listener(new StepExecutionListener() {
                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        stats.addErrorCount(stepExecution.getWriteSkipCount());
                        // The insert failure that breaches the limit is not
                        // skipped (it aborts the step); count and log it once
                        // here so RETURN-CODE is 101 like the validation path.
                        if (stepExecution.getStatus() == BatchStatus.FAILED
                                && stepExecution.getFailureExceptions().stream()
                                        .anyMatch(HistoryLoadJobConfig::causedByDataAccess)) {
                            stats.incrementErrorCount();
                            errorHandlingService.logError(PROGRAM_ID, "S", 3, "HIST0002",
                                    "POSHIST insert failed: error limit exceeded", PROGRAM_ID);
                        }
                        return stepExecution.getExitStatus();
                    }
                })
                .listener(new ChunkListener() {
                    @Override
                    public void afterChunk(ChunkContext context) {
                        JobParameters params = context.getStepContext()
                                .getStepExecution().getJobParameters();
                        batchControlService.updateCheckpoint(
                                PROGRAM_ID,
                                params.getString("processDate"),
                                stats.getRecordsRead(),
                                stats.getRecordsWritten());
                    }
                })
                .build();
    }

    private static boolean causedByDataAccess(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof DataAccessException) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public Job histld00Job(JobRepository jobRepository,
                           Step histld00Step,
                           BatchControlService batchControlService,
                           HistoryLoadStats stats) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        stats.reset();
                        batchControlService.markActive(PROGRAM_ID,
                                jobExecution.getJobParameters().getString("processDate"));
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        batchControlService.markComplete(PROGRAM_ID,
                                jobExecution.getJobParameters().getString("processDate"),
                                stats.getRecordsRead(),
                                stats.getRecordsWritten(),
                                (int) Math.min(stats.getErrorCount(), Integer.MAX_VALUE),
                                jobExecution.getStatus() == BatchStatus.FAILED);
                    }
                })
                .start(histld00Step)
                .build();
    }
}
