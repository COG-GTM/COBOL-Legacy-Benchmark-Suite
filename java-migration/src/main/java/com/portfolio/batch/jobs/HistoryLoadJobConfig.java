package com.portfolio.batch.jobs;

import com.portfolio.model.entity.PositionHistory;
import com.portfolio.model.entity.Transaction;
import com.portfolio.batch.listeners.CheckpointRestartListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import com.portfolio.repository.TransactionRepository;
import com.portfolio.repository.PositionHistoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class HistoryLoadJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransactionRepository transactionRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final CheckpointRestartListener checkpointRestartListener;

    public HistoryLoadJobConfig(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                TransactionRepository transactionRepository,
                                PositionHistoryRepository positionHistoryRepository,
                                CheckpointRestartListener checkpointRestartListener) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.transactionRepository = transactionRepository;
        this.positionHistoryRepository = positionHistoryRepository;
        this.checkpointRestartListener = checkpointRestartListener;
    }

    @Bean
    public Job historyLoadJob() {
        return new JobBuilder("historyLoadJob", jobRepository)
                .start(historyLoadStep())
                .build();
    }

    @Bean
    public Step historyLoadStep() {
        return new StepBuilder("historyLoadStep", jobRepository)
                .<Transaction, PositionHistory>chunk(1000, transactionManager)
                .reader(historyReader())
                .processor(historyProcessor())
                .writer(historyWriter())
                .faultTolerant()
                .skip(DataIntegrityViolationException.class)
                .skipLimit(100)
                .listener(checkpointRestartListener)
                .build();
    }

    @Bean
    public RepositoryItemReader<Transaction> historyReader() {
        return new RepositoryItemReaderBuilder<Transaction>()
                .name("transactionReader")
                .repository(transactionRepository)
                .methodName("findAll")
                .sorts(Map.of("transactionId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, PositionHistory> historyProcessor() {
        return transaction -> {
            PositionHistory history = new PositionHistory();
            history.setAccountNo(transaction.getPortfolioId().substring(0,
                    Math.min(8, transaction.getPortfolioId().length())));
            history.setPortfolioId(transaction.getPortfolioId());
            history.setTransDate(transaction.getTransactionDate());
            history.setTransTime(transaction.getTransactionTime());
            history.setTransType(transaction.getTransactionType());
            history.setSecurityId(transaction.getInvestmentId());
            history.setQuantity(transaction.getQuantity());
            history.setPrice(transaction.getPrice());
            history.setAmount(transaction.getAmount());
            history.setFees(BigDecimal.ZERO);
            history.setTotalAmount(transaction.getAmount());
            history.setCostBasis(transaction.getAmount());
            history.setGainLoss(BigDecimal.ZERO);
            history.setProcessDate(transaction.getTransactionDate());
            history.setProcessTime(transaction.getTransactionTime());
            history.setProgramId("HISTLD00");
            history.setUserId(transaction.getProcessUser());
            history.setAuditTimestamp(LocalDateTime.now());
            return history;
        };
    }

    @Bean
    public RepositoryItemWriter<PositionHistory> historyWriter() {
        return new RepositoryItemWriterBuilder<PositionHistory>()
                .repository(positionHistoryRepository)
                .methodName("save")
                .build();
    }
}
