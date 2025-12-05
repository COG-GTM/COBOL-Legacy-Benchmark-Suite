package com.portfolio.batch;

import com.portfolio.entity.TransactionRecord;
import com.portfolio.repository.TransactionRecordRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch Job Configuration for Transaction Record Migration
 * Migrates data from VSAM TRANHIST flat file export to PostgreSQL
 */
@Configuration
public class TransactionMigrationJobConfig {

    @Value("${portfolio.batch.chunk-size:1000}")
    private int chunkSize;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionRecordRepository transactionRecordRepository;

    public TransactionMigrationJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TransactionRecordRepository transactionRecordRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    /**
     * Transaction Migration Job
     */
    @Bean
    public Job transactionMigrationJob(Step transactionMigrationStep) {
        return new JobBuilder("transactionMigrationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(transactionMigrationStep)
                .listener(new MigrationJobListener())
                .build();
    }

    /**
     * Transaction Migration Step
     */
    @Bean
    public Step transactionMigrationStep(
            ItemReader<TransactionInputRecord> transactionReader,
            ItemProcessor<TransactionInputRecord, TransactionRecord> transactionProcessor,
            ItemWriter<TransactionRecord> transactionWriter) {
        return new StepBuilder("transactionMigrationStep", jobRepository)
                .<TransactionInputRecord, TransactionRecord>chunk(chunkSize, transactionManager)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(100)
                .skip(Exception.class)
                .listener(new MigrationStepListener())
                .build();
    }

    /**
     * Reader for VSAM TRANHIST flat file export
     * Reads fixed-length records matching COBOL TRNREC.cpy layout
     */
    @Bean
    public FlatFileItemReader<TransactionInputRecord> transactionReader(
            @Value("${portfolio.migration.transaction-file:data/transaction.dat}") String filePath) {
        return new FlatFileItemReaderBuilder<TransactionInputRecord>()
                .name("transactionReader")
                .resource(new FileSystemResource(filePath))
                .fixedLength()
                .columns(new org.springframework.batch.item.file.transform.Range[]{
                        new org.springframework.batch.item.file.transform.Range(1, 8),    // TRN-DATE
                        new org.springframework.batch.item.file.transform.Range(9, 14),   // TRN-TIME
                        new org.springframework.batch.item.file.transform.Range(15, 22),  // TRN-PORTFOLIO-ID
                        new org.springframework.batch.item.file.transform.Range(23, 28),  // TRN-SEQUENCE-NO
                        new org.springframework.batch.item.file.transform.Range(29, 38),  // TRN-INVESTMENT-ID
                        new org.springframework.batch.item.file.transform.Range(39, 40),  // TRN-TYPE
                        new org.springframework.batch.item.file.transform.Range(41, 56),  // TRN-QUANTITY (COMP-3)
                        new org.springframework.batch.item.file.transform.Range(57, 72),  // TRN-PRICE (COMP-3)
                        new org.springframework.batch.item.file.transform.Range(73, 88),  // TRN-AMOUNT (COMP-3)
                        new org.springframework.batch.item.file.transform.Range(89, 91),  // TRN-CURRENCY
                        new org.springframework.batch.item.file.transform.Range(92, 92)   // TRN-STATUS
                })
                .names("transactionDate", "transactionTime", "portfolioId", "sequenceNo",
                       "investmentId", "transactionType", "quantity", "price",
                       "amount", "currencyCode", "status")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TransactionInputRecord.class);
                }})
                .build();
    }

    /**
     * Processor to transform COBOL record to JPA entity
     */
    @Bean
    public ItemProcessor<TransactionInputRecord, TransactionRecord> transactionProcessor() {
        return new TransactionRecordProcessor();
    }

    /**
     * Writer to persist transaction entities to PostgreSQL
     */
    @Bean
    public ItemWriter<TransactionRecord> transactionWriter() {
        return items -> {
            for (TransactionRecord transaction : items) {
                transactionRecordRepository.save(transaction);
            }
        };
    }
}
