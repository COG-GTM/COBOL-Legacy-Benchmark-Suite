package com.clbs.position.batch;

import com.clbs.position.entity.Transaction;
import com.clbs.position.service.PositionUpdateService;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch translation of the COBOL batch driver and its checkpoint/restart
 * framework (copybook {@code src/copybook/batch/CKPRST.cpy}, program
 * {@code src/programs/batch/CKPRST.cbl}).
 *
 * <h2>COBOL checkpoint/restart &rarr; Spring Batch mapping</h2>
 * <ul>
 *   <li>{@code CK-COMMIT-FREQ} (commit every N records) &rarr; the chunk size.
 *       The data dictionary (8.3) specifies <b>"Position update: Every 500
 *       updates"</b>, so the chunk/commit interval is {@value #CHUNK_SIZE}.</li>
 *   <li>{@code CALL 'CKPTAKE'} / {@code CALL 'CKPCMIT'} (take/commit a
 *       checkpoint) &rarr; Spring Batch committing a chunk and persisting the
 *       step's read count to the {@code JobRepository}.</li>
 *   <li>{@code CK-LAST-KEY} / {@code CHK-RECORDS-PROC} (last committed position)
 *       &rarr; the {@code BATCH_STEP_EXECUTION} read/write counts.</li>
 *   <li>{@code CALL 'CKPRSTR'} with {@code CK-MODE-RESTART} &rarr; relaunching
 *       the job with the same identifying {@code JobParameters}; Spring Batch
 *       resumes after the last committed chunk. The per-item update is
 *       idempotent (already-processed transactions are filtered), mirroring the
 *       COBOL re-drive from the last checkpoint.</li>
 * </ul>
 */
@Configuration
public class PositionUpdateJobConfig {

    public static final String JOB_NAME = "positionUpdateJob";
    public static final String STEP_NAME = "applyTransactionsStep";

    /** Commit/checkpoint interval &mdash; data-dictionary 8.3 "Every 500 updates". */
    public static final int CHUNK_SIZE = 500;

    /** Pending-transaction status code ({@code TRN-STATUS} 'P'). */
    private static final String STATUS_PENDING = "P";

    @Bean
    public JpaPagingItemReader<Transaction> transactionReader(EntityManagerFactory emf) {
        // Read every transaction in stable id order. The set the query returns
        // never changes as rows are processed (status mutates but rows are not
        // filtered out of this query), so paging offsets stay correct across
        // chunk commits and restarts.
        return new JpaPagingItemReaderBuilder<Transaction>()
                .name("transactionReader")
                .entityManagerFactory(emf)
                .queryString("SELECT t FROM Transaction t ORDER BY t.id")
                .pageSize(CHUNK_SIZE)
                .saveState(true)
                .build();
    }

    /** Skips transactions that are not pending (idempotent restart). */
    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        return txn -> STATUS_PENDING.equals(txn.getStatus()) ? txn : null;
    }

    @Bean
    public ItemWriter<Transaction> transactionWriter(PositionUpdateService service) {
        return chunk -> {
            for (Transaction txn : chunk.getItems()) {
                service.applyTransaction(txn);
            }
        };
    }

    @Bean
    public Step applyTransactionsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaPagingItemReader<Transaction> transactionReader,
            ItemProcessor<Transaction, Transaction> transactionProcessor,
            ItemWriter<Transaction> transactionWriter) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Transaction, Transaction>chunk(CHUNK_SIZE, transactionManager)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .build();
    }

    @Bean
    public Job positionUpdateJob(JobRepository jobRepository, Step applyTransactionsStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(applyTransactionsStep)
                .build();
    }
}
