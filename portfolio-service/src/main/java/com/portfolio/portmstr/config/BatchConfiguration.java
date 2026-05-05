package com.portfolio.portmstr.config;

import com.portfolio.portmstr.batch.PortfolioBatchProcessor;
import com.portfolio.portmstr.batch.listener.BatchJobListener;
import com.portfolio.portmstr.dto.PortfolioRequest;
import com.portfolio.portmstr.model.PortfolioMaster;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.Collections;

/**
 * Spring Batch configuration.
 * Replaces COBOL JCL job definitions and batch step sequencing.
 *
 * JCL mapping:
 *   //PORTADD  EXEC PGM=PORTADD  -> portfolioAddJob / portfolioAddStep
 *   //PORTUPDT EXEC PGM=PORTUPDT -> (same job, different reader)
 *   //PORTDEL  EXEC PGM=PORTDEL  -> (same job, different reader)
 *
 * The chunk size maps to COBOL CK-COMMIT-FREQ from CKPRST.cpy.
 */
@Configuration
public class BatchConfiguration {

    @Value("${portfolio.batch.chunk-size:100}")
    private int chunkSize;

    @Bean
    public Job portfolioProcessingJob(JobRepository jobRepository,
                                       Step portfolioProcessingStep,
                                       BatchJobListener listener) {
        return new JobBuilder("portfolioProcessingJob", jobRepository)
                .listener(listener)
                .start(portfolioProcessingStep)
                .build();
    }

    @Bean
    public Step portfolioProcessingStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         PortfolioBatchProcessor processor) {
        return new StepBuilder("portfolioProcessingStep", jobRepository)
                .<PortfolioRequest, PortfolioMaster>chunk(chunkSize, transactionManager)
                .reader(defaultPortfolioReader())
                .processor(processor)
                .writer(noOpWriter())
                .build();
    }

    @Bean
    public ItemReader<PortfolioRequest> defaultPortfolioReader() {
        return new ListItemReader<>(Collections.emptyList());
    }

    private ItemWriter<PortfolioMaster> noOpWriter() {
        return items -> {
            // Writing is handled within the processor via PortfolioMasterService
        };
    }
}
