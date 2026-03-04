package com.cobolbenchmark.batch;

import com.cobolbenchmark.model.PoshistRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * History Load Job Configuration - migrated from HISTLD00.cbl.
 * Chunk-oriented step with chunk(1000) - from commit-every-1000 pattern.
 * SQLCODE=-803 duplicate handling via SkipPolicy ignoring DuplicateKeyException.
 * EXEC SQL INSERT INTO POSHIST → JdbcBatchItemWriter.
 */
@Configuration
public class HistoryLoadJobConfig {

    @Value("${app.batch.commit-interval:1000}")
    private int commitInterval;

    @Value("${app.batch.max-errors:100}")
    private int maxErrors;

    @Bean
    public Job historyLoadJob(JobRepository jobRepository, Step historyLoadStep) {
        return new JobBuilder("historyLoadJob", jobRepository)
                .start(historyLoadStep)
                .build();
    }

    @Bean
    public Step historyLoadStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                FlatFileItemReader<PoshistRecord> historyReader,
                                HistoryLoadProcessor historyProcessor,
                                JdbcBatchItemWriter<PoshistRecord> historyWriter) {
        return new StepBuilder("historyLoadStep", jobRepository)
                .<PoshistRecord, PoshistRecord>chunk(commitInterval, transactionManager)
                .reader(historyReader)
                .processor(historyProcessor)
                .writer(historyWriter)
                .faultTolerant()
                .skip(DuplicateKeyException.class)
                .skipLimit(maxErrors)
                .build();
    }

    @Bean
    public FlatFileItemReader<PoshistRecord> historyReader(
            @Value("${app.batch.history-file:input/history.dat}") String filePath) {
        return new FlatFileItemReaderBuilder<PoshistRecord>()
                .name("historyReader")
                .resource(new FileSystemResource(filePath))
                .delimited()
                .names("accountNo", "portfolioId", "transType", "securityId",
                       "quantity", "price", "amount", "fees", "totalAmount",
                       "costBasis", "gainLoss", "programId", "userId")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(PoshistRecord.class);
                }})
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<PoshistRecord> historyWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<PoshistRecord>()
                .sql("INSERT INTO POSHIST (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME, " +
                     "TRANS_TYPE, SECURITY_ID, QUANTITY, PRICE, AMOUNT, FEES, TOTAL_AMOUNT, " +
                     "COST_BASIS, GAIN_LOSS, PROCESS_DATE, PROCESS_TIME, PROGRAM_ID, USER_ID, AUDIT_TIMESTAMP) " +
                     "VALUES (:accountNo, :portfolioId, :transDate, :transTime, " +
                     ":transType, :securityId, :quantity, :price, :amount, :fees, :totalAmount, " +
                     ":costBasis, :gainLoss, :processDate, :processTime, :programId, :userId, :auditTimestamp)")
                .dataSource(dataSource)
                .beanMapped()
                .build();
    }
}
