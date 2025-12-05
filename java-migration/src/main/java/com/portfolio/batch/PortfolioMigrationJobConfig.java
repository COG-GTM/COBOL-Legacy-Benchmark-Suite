package com.portfolio.batch;

import com.portfolio.entity.PortfolioMaster;
import com.portfolio.repository.PortfolioMasterRepository;
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
 * Spring Batch Job Configuration for Portfolio Master Migration
 * Migrates data from VSAM PORTMSTR flat file export to PostgreSQL
 */
@Configuration
public class PortfolioMigrationJobConfig {

    @Value("${portfolio.batch.chunk-size:1000}")
    private int chunkSize;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PortfolioMasterRepository portfolioMasterRepository;

    public PortfolioMigrationJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PortfolioMasterRepository portfolioMasterRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.portfolioMasterRepository = portfolioMasterRepository;
    }

    /**
     * Portfolio Migration Job
     */
    @Bean
    public Job portfolioMigrationJob(Step portfolioMigrationStep) {
        return new JobBuilder("portfolioMigrationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(portfolioMigrationStep)
                .listener(new MigrationJobListener())
                .build();
    }

    /**
     * Portfolio Migration Step
     */
    @Bean
    public Step portfolioMigrationStep(
            ItemReader<PortfolioInputRecord> portfolioReader,
            ItemProcessor<PortfolioInputRecord, PortfolioMaster> portfolioProcessor,
            ItemWriter<PortfolioMaster> portfolioWriter) {
        return new StepBuilder("portfolioMigrationStep", jobRepository)
                .<PortfolioInputRecord, PortfolioMaster>chunk(chunkSize, transactionManager)
                .reader(portfolioReader)
                .processor(portfolioProcessor)
                .writer(portfolioWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(100)
                .skip(Exception.class)
                .listener(new MigrationStepListener())
                .build();
    }

    /**
     * Reader for VSAM PORTMSTR flat file export
     * Reads fixed-length records matching COBOL PORTFLIO.cpy layout
     */
    @Bean
    public FlatFileItemReader<PortfolioInputRecord> portfolioReader(
            @Value("${portfolio.migration.portfolio-file:data/portfolio.dat}") String filePath) {
        return new FlatFileItemReaderBuilder<PortfolioInputRecord>()
                .name("portfolioReader")
                .resource(new FileSystemResource(filePath))
                .fixedLength()
                .columns(new org.springframework.batch.item.file.transform.Range[]{
                        new org.springframework.batch.item.file.transform.Range(1, 8),    // PORT-ID
                        new org.springframework.batch.item.file.transform.Range(9, 18),   // PORT-ACCOUNT-NO
                        new org.springframework.batch.item.file.transform.Range(19, 48),  // PORT-CLIENT-NAME
                        new org.springframework.batch.item.file.transform.Range(49, 49),  // PORT-CLIENT-TYPE
                        new org.springframework.batch.item.file.transform.Range(50, 57),  // PORT-CREATE-DATE
                        new org.springframework.batch.item.file.transform.Range(58, 65),  // PORT-LAST-MAINT
                        new org.springframework.batch.item.file.transform.Range(66, 66),  // PORT-STATUS
                        new org.springframework.batch.item.file.transform.Range(67, 82),  // PORT-TOTAL-VALUE (COMP-3)
                        new org.springframework.batch.item.file.transform.Range(83, 98),  // PORT-CASH-BALANCE (COMP-3)
                        new org.springframework.batch.item.file.transform.Range(99, 106), // PORT-LAST-USER
                        new org.springframework.batch.item.file.transform.Range(107, 114) // PORT-LAST-TRANS
                })
                .names("portfolioId", "accountNo", "clientName", "clientType", 
                       "createDate", "lastMaint", "status", "totalValue", 
                       "cashBalance", "lastUser", "lastTrans")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(PortfolioInputRecord.class);
                }})
                .build();
    }

    /**
     * Processor to transform COBOL record to JPA entity
     */
    @Bean
    public ItemProcessor<PortfolioInputRecord, PortfolioMaster> portfolioProcessor() {
        return new PortfolioRecordProcessor();
    }

    /**
     * Writer to persist portfolio entities to PostgreSQL
     */
    @Bean
    public ItemWriter<PortfolioMaster> portfolioWriter() {
        return items -> {
            for (PortfolioMaster portfolio : items) {
                portfolioMasterRepository.save(portfolio);
            }
        };
    }
}
