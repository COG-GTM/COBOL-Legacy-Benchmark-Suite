package com.clbs.portfolio.batch.config;

import com.clbs.portfolio.batch.processor.AdjudicationProcessor;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AdjudicationJobConfig {

    private final TransactionRecordRepository transactionRecordRepository;
    private final AdjudicationProcessor adjudicationProcessor;

    @Bean
    public RepositoryItemReader<TransactionRecord> adjudicationReader() {
        return new RepositoryItemReaderBuilder<TransactionRecord>()
                .name("adjudicationReader")
                .repository(transactionRecordRepository)
                .methodName("findByStatus")
                .arguments(Collections.singletonList("DONE"))
                .sorts(Map.of("id", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemWriter<TransactionRecord> adjudicationWriter() {
        return items -> {
            for (TransactionRecord item : items) {
                transactionRecordRepository.save(item);
            }
        };
    }

    @Bean
    public Step adjudicationStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("adjudicationStep", jobRepository)
                .<TransactionRecord, TransactionRecord>chunk(100, transactionManager)
                .reader(adjudicationReader())
                .processor(adjudicationProcessor)
                .writer(adjudicationWriter())
                .build();
    }
}
