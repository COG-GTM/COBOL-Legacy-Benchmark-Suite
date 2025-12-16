package com.portfolio.batch;

import com.portfolio.entity.Transaction;
import com.portfolio.entity.Transaction.TransactionStatus;
import com.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.util.Collections;

/**
 * Spring Batch configuration for transaction processing.
 * Replaces COBOL batch programs TRNVAL00 and POSUPD00.
 * 
 * @see src/programs/batch/TRNVAL00.cbl - Transaction Validation
 * @see src/programs/batch/POSUPD00.cbl - Position Update
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessingJob {

    private final TransactionRepository transactionRepository;

    @Value("${application.batch.chunk-size:1000}")
    private int chunkSize;

    @Bean
    public Job processTransactionsJob(JobRepository jobRepository, Step processTransactionsStep) {
        return new JobBuilder("processTransactionsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(processTransactionsStep)
                .build();
    }

    @Bean
    public Step processTransactionsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Transaction> pendingTransactionReader,
            ItemProcessor<Transaction, Transaction> transactionProcessor,
            ItemWriter<Transaction> transactionWriter) {
        
        return new StepBuilder("processTransactionsStep", jobRepository)
                .<Transaction, Transaction>chunk(chunkSize, transactionManager)
                .reader(pendingTransactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public RepositoryItemReader<Transaction> pendingTransactionReader() {
        return new RepositoryItemReaderBuilder<Transaction>()
                .name("pendingTransactionReader")
                .repository(transactionRepository)
                .methodName("findByStatus")
                .arguments(TransactionStatus.PENDING)
                .sorts(Collections.singletonMap("transactionDate", Sort.Direction.ASC))
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> transactionProcessor() {
        return transaction -> {
            log.debug("Processing transaction: {}", transaction.getTransactionId());
            
            try {
                validateTransaction(transaction);
                
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setProcessDate(OffsetDateTime.now());
                transaction.setProcessUser("BATCH");
                transaction.setResultCode("0000");
                
                log.info("Transaction processed successfully: {}", transaction.getTransactionId());
                
            } catch (Exception e) {
                log.error("Transaction processing failed: {}", transaction.getTransactionId(), e);
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setResultCode("9999");
                transaction.setProcessDate(OffsetDateTime.now());
                transaction.setProcessUser("BATCH");
            }
            
            return transaction;
        };
    }

    @Bean
    public ItemWriter<Transaction> transactionWriter() {
        return transactions -> {
            for (Transaction transaction : transactions) {
                transactionRepository.save(transaction);
            }
            log.info("Batch of {} transactions written", transactions.size());
        };
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().isBlank()) {
            throw new IllegalArgumentException("Portfolio ID is required");
        }
        if (transaction.getInvestmentId() == null || transaction.getInvestmentId().isBlank()) {
            throw new IllegalArgumentException("Investment ID is required");
        }
        if (transaction.getQuantity() == null || transaction.getQuantity().signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (transaction.getPrice() == null || transaction.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
}
