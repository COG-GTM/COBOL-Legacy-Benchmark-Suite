package com.clbs.portfolio.batch.config;

import com.clbs.portfolio.batch.processor.TransactionValidationProcessor;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
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
public class TransactionValidationJobConfig {

    private final TransactionRecordRepository transactionRecordRepository;
    private final TransactionValidationProcessor validationProcessor;

    @Bean
    public RepositoryItemReader<TransactionRecord> validationReader() {
        return new RepositoryItemReaderBuilder<TransactionRecord>()
                .name("validationReader")
                .repository(transactionRecordRepository)
                .methodName("findByStatus")
                .arguments(Collections.singletonList("PENDING"))
                .sorts(Map.of("id", Sort.Direction.ASC))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemWriter<TransactionRecord> validationWriter() {
        return items -> {
            for (TransactionRecord item : items) {
                transactionRecordRepository.save(item);
            }
        };
    }

    @Bean
    public Step transactionValidationStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager) {
        return new StepBuilder("transactionValidationStep", jobRepository)
                .<TransactionRecord, TransactionRecord>chunk(100, transactionManager)
                .reader(validationReader())
                .processor(validationProcessor)
                .writer(validationWriter())
                .build();
    }
}
