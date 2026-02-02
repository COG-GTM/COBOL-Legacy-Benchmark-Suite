package com.portfolio.service.batch;

import com.portfolio.domain.BatchControl;
import com.portfolio.domain.HistoryRecord;
import com.portfolio.domain.Transaction;
import com.portfolio.repository.BatchControlRepository;
import com.portfolio.repository.HistoryRecordRepository;
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
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * History Load Batch Job - migrated from COBOL HISTLD00
 * Loads transaction history from VSAM to DB2 (now from transactions to history_records)
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class HistoryLoadJob {

    private final TransactionRepository transactionRepository;
    private final HistoryRecordRepository historyRecordRepository;
    private final BatchControlRepository batchControlRepository;

    @Bean("historyLoadJobBean")
    public Job historyLoadJobBean(JobRepository jobRepository, Step historyLoadStep) {
        return new JobBuilder("historyLoadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(historyLoadStep)
                .build();
    }

    @Bean
    public Step historyLoadStep(JobRepository jobRepository, 
                                 PlatformTransactionManager transactionManager) {
        return new StepBuilder("historyLoadStep", jobRepository)
                .<Transaction, HistoryRecord>chunk(1000, transactionManager)
                .reader(transactionReader())
                .processor(transactionToHistoryProcessor())
                .writer(historyWriter())
                .build();
    }

    @Bean
    public ItemReader<Transaction> transactionReader() {
        List<Transaction> completedTransactions = transactionRepository
                .findByStatus(Transaction.TransactionStatus.D);
        return new ListItemReader<>(completedTransactions);
    }

    @Bean
    public ItemProcessor<Transaction, HistoryRecord> transactionToHistoryProcessor() {
        return transaction -> {
            log.debug("Processing transaction: {}", transaction.getId());
            
            return HistoryRecord.builder()
                    .portfolioId(transaction.getPortfolioId())
                    .historyDate(transaction.getTransactionDate())
                    .historyTime(transaction.getTransactionTime() != null ? 
                            transaction.getTransactionTime() : LocalTime.now())
                    .recordType(HistoryRecord.RecordType.TR)
                    .actionCode(HistoryRecord.ActionCode.A)
                    .afterImage(serializeTransaction(transaction))
                    .processDate(LocalDateTime.now())
                    .processUser("BATCH")
                    .build();
        };
    }

    @Bean
    public ItemWriter<HistoryRecord> historyWriter() {
        return items -> {
            for (HistoryRecord record : items) {
                historyRecordRepository.save(record);
            }
            log.info("Written {} history records", items.size());
        };
    }

    private String serializeTransaction(Transaction transaction) {
        return String.format("ID:%d,TYPE:%s,AMT:%s,QTY:%s,INV:%s,DATE:%s",
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getQuantity(),
                transaction.getInvestmentId(),
                transaction.getTransactionDate());
    }

    public void initializeBatchControl(String jobName) {
        BatchControl control = BatchControl.builder()
                .jobName(jobName)
                .processDate(LocalDate.now())
                .status(BatchControl.BatchStatus.R)
                .programName("HISTLD00")
                .build();
        batchControlRepository.save(control);
    }

    public void updateBatchControl(String jobName, BatchControl.BatchStatus status, 
                                    long recordsRead, long recordsWritten, long recordsError) {
        batchControlRepository.findByJobNameAndProcessDate(jobName, LocalDate.now())
                .ifPresent(control -> {
                    control.setStatus(status);
                    control.setRecordsRead(recordsRead);
                    control.setRecordsWritten(recordsWritten);
                    control.setRecordsError(recordsError);
                    control.setCompleteTimestamp(LocalDateTime.now());
                    control.setReturnCode(recordsError > 0 ? 8 : 0);
                    batchControlRepository.save(control);
                });
    }
}
