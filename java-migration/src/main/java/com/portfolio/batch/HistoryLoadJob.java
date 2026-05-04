package com.portfolio.batch;

import com.portfolio.domain.PosHistRecord;
import com.portfolio.domain.Transaction;
import com.portfolio.repository.PosHistRepository;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.service.common.ErrorProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * History Load Job - migrated from COBOL HISTLD00.cbl.
 * ItemReader reads from transaction history (now DB table).
 * ItemWriter inserts into POSHIST via JPA.
 * Commit threshold (WS-COMMIT-THRESHOLD = 1000) -> Spring Batch chunk(1000).
 * SQLCODE -803 (duplicate) -> catch DataIntegrityViolationException and skip.
 */
@Configuration
public class HistoryLoadJob {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadJob.class);
    private static final int CHUNK_SIZE = 1000;

    private final TransactionRepository transactionRepository;
    private final PosHistRepository posHistRepository;
    private final ErrorProcessingService errorProcessingService;

    public HistoryLoadJob(TransactionRepository transactionRepository,
                          PosHistRepository posHistRepository,
                          ErrorProcessingService errorProcessingService) {
        this.transactionRepository = transactionRepository;
        this.posHistRepository = posHistRepository;
        this.errorProcessingService = errorProcessingService;
    }

    @Bean
    public Job historyLoadBatchJob(JobRepository jobRepository, Step historyLoadStep) {
        return new JobBuilder("historyLoadJob", jobRepository)
                .start(historyLoadStep)
                .build();
    }

    @Bean
    public Step historyLoadStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager) {
        return new StepBuilder("historyLoadStep", jobRepository)
                .<Transaction, PosHistRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(transactionItemReader())
                .processor(transactionToHistoryProcessor())
                .writer(posHistItemWriter())
                .faultTolerant()
                .skip(DataIntegrityViolationException.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    public ItemReader<Transaction> transactionItemReader() {
        return new ListItemReader<>(transactionRepository.findByStatus("D"));
    }

    @Bean
    public ItemProcessor<Transaction, PosHistRecord> transactionToHistoryProcessor() {
        return transaction -> {
            PosHistRecord record = new PosHistRecord();
            record.setAccountNo(transaction.getPortfolioId());
            record.setPortfolioId(transaction.getPortfolioId());
            record.setTransDate(transaction.getTransactionDate());
            record.setTransTime(transaction.getTransactionTime() != null
                    ? transaction.getTransactionTime() : "000000");
            record.setTransType(transaction.getTransactionType());
            record.setSecurityId(transaction.getInvestmentId());
            record.setQuantity(transaction.getQuantity());
            record.setPrice(transaction.getPrice());
            record.setAmount(transaction.getAmount());
            record.setFees(BigDecimal.ZERO);
            record.setTotalAmount(transaction.getAmount());
            record.setCostBasis(transaction.getAmount());
            record.setGainLoss(BigDecimal.ZERO);
            record.setProcessDate(LocalDate.now());
            record.setProcessTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            record.setProgramId("HISTLD00");
            record.setUserId("BATCH");
            record.setAuditTimestamp(LocalDateTime.now());
            return record;
        };
    }

    @Bean
    public ItemWriter<PosHistRecord> posHistItemWriter() {
        return items -> {
            for (PosHistRecord item : items) {
                try {
                    posHistRepository.save(item);
                } catch (DataIntegrityViolationException e) {
                    log.warn("Duplicate record skipped: {}/{}", item.getAccountNo(), item.getTransDate());
                }
            }
        };
    }
}
