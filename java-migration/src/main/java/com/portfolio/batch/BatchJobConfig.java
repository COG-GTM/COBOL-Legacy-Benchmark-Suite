package com.portfolio.batch;

import com.portfolio.entity.*;
import com.portfolio.repository.*;
import com.portfolio.service.AuditProcessor;
import com.portfolio.service.DatabaseErrorHandler;
import com.portfolio.util.BatchConstants;
import com.portfolio.util.CommonConstants;
import com.portfolio.util.PortfolioValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class BatchJobConfig {

    private static final Logger log = LoggerFactory.getLogger(BatchJobConfig.class);

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final HistoryRepository historyRepository;
    private final BatchControlRepository batchControlRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditProcessor auditProcessor;
    private final DatabaseErrorHandler errorHandler;

    public BatchJobConfig(TransactionRepository transactionRepository,
                          PortfolioRepository portfolioRepository,
                          PositionRepository positionRepository,
                          HistoryRepository historyRepository,
                          BatchControlRepository batchControlRepository,
                          AuditLogRepository auditLogRepository,
                          AuditProcessor auditProcessor,
                          DatabaseErrorHandler errorHandler) {
        this.transactionRepository = transactionRepository;
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.historyRepository = historyRepository;
        this.batchControlRepository = batchControlRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditProcessor = auditProcessor;
        this.errorHandler = errorHandler;
    }

    @Bean
    public Job dailyBatchJob(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new JobBuilder("dailyBatchJob", jobRepository)
                .start(transactionValidationStep(jobRepository, txManager))
                .next(positionUpdateStep(jobRepository, txManager))
                .next(historyLoadStep(jobRepository, txManager))
                .next(reportGenerationStep(jobRepository, txManager))
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        log.info("Daily batch job starting...");
                        BatchControl control = new BatchControl();
                        control.setJobName("DAILY");
                        control.setProcessDate(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
                        control.setStatus(BatchConstants.STATUS_ACTIVE);
                        control.setStartTime(LocalDateTime.now());
                        control.setProgramName("BCHCTL00");
                        batchControlRepository.save(control);
                        auditProcessor.logSystemEvent("BCHCTL00", "STARTUP", "Daily batch job started");
                    }
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        String status = jobExecution.getStatus() == BatchStatus.COMPLETED
                                ? BatchConstants.STATUS_DONE : BatchConstants.STATUS_ERROR;
                        log.info("Daily batch job completed with status: {}", status);
                        auditProcessor.logSystemEvent("BCHCTL00", "SHUTDOWN",
                                "Daily batch job completed: " + jobExecution.getStatus());
                    }
                })
                .build();
    }

    @Bean
    public Step transactionValidationStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("transactionValidation", jobRepository)
                .<TransactionRecord, TransactionRecord>chunk(100, txManager)
                .reader(pendingTransactionReader())
                .processor(transactionValidator())
                .writer(transactionWriter())
                .listener(new StepExecutionListener() {
                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.info("Transaction validation: read={}, processed={}, errors={}",
                                stepExecution.getReadCount(), stepExecution.getWriteCount(),
                                stepExecution.getSkipCount());
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }

    @Bean
    public Step positionUpdateStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("positionUpdate", jobRepository)
                .<TransactionRecord, PositionRecord>chunk(100, txManager)
                .reader(completedTransactionReader())
                .processor(positionUpdateProcessor())
                .writer(positionWriter())
                .build();
    }

    @Bean
    public Step historyLoadStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("historyLoad", jobRepository)
                .<TransactionRecord, HistoryRecord>chunk(100, txManager)
                .reader(completedTransactionReaderForHistory())
                .processor(historyLoadProcessor())
                .writer(historyWriter())
                .build();
    }

    @Bean
    public Step reportGenerationStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("reportGeneration", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("=== POSITION REPORT ===");
                    List<Portfolio> portfolios = portfolioRepository.findActivePortfolios();
                    for (Portfolio p : portfolios) {
                        List<PositionRecord> positions = positionRepository.findActivePositions(p.getPortfolioId());
                        BigDecimal totalValue = positions.stream()
                                .map(PositionRecord::getMarketValue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        log.info("Portfolio: {} | Positions: {} | Total Value: {}",
                                p.getPortfolioId(), positions.size(), totalValue);
                    }
                    log.info("=== AUDIT REPORT ===");
                    log.info("Total audit entries: {}", auditLogRepository.count());
                    log.info("=== STATISTICS REPORT ===");
                    log.info("Total portfolios: {}", portfolioRepository.count());
                    log.info("Total positions: {}", positionRepository.count());
                    log.info("Total transactions: {}", transactionRepository.count());
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }

    private ItemReader<TransactionRecord> pendingTransactionReader() {
        return new ItemReader<>() {
            private Iterator<TransactionRecord> iterator;
            private boolean initialized = false;
            @Override
            public TransactionRecord read() {
                if (!initialized) {
                    iterator = transactionRepository.findByStatus("P").iterator();
                    initialized = true;
                }
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                initialized = false;
                return null;
            }
        };
    }

    private ItemProcessor<TransactionRecord, TransactionRecord> transactionValidator() {
        return transaction -> {
            List<String> errors = PortfolioValidation.validateTransaction(transaction);
            if (!errors.isEmpty()) {
                log.warn("Transaction validation failed for {}: {}", transaction.getTransactionId(), errors);
                transaction.setStatus("F");
                transactionRepository.save(transaction);
                return null;
            }
            if (!portfolioRepository.existsById(transaction.getPortfolioId())) {
                log.warn("Portfolio not found for transaction: {}", transaction.getTransactionId());
                transaction.setStatus("F");
                transactionRepository.save(transaction);
                return null;
            }
            transaction.setStatus("D");
            transaction.setProcessDate(LocalDateTime.now());
            transaction.setProcessUser("BATCH");
            return transaction;
        };
    }

    private ItemWriter<TransactionRecord> transactionWriter() {
        return chunk -> {
            for (TransactionRecord t : chunk.getItems()) {
                transactionRepository.save(t);
            }
        };
    }

    private ItemReader<TransactionRecord> completedTransactionReader() {
        return new ItemReader<>() {
            private Iterator<TransactionRecord> iterator;
            private boolean initialized = false;
            @Override
            public TransactionRecord read() {
                if (!initialized) {
                    iterator = transactionRepository.findByStatus("D").iterator();
                    initialized = true;
                }
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                initialized = false;
                return null;
            }
        };
    }

    private ItemReader<TransactionRecord> completedTransactionReaderForHistory() {
        return new ItemReader<>() {
            private Iterator<TransactionRecord> iterator;
            private boolean initialized = false;
            @Override
            public TransactionRecord read() {
                if (!initialized) {
                    iterator = transactionRepository.findByStatus("U").iterator();
                    initialized = true;
                }
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                initialized = false;
                return null;
            }
        };
    }

    private ItemProcessor<TransactionRecord, PositionRecord> positionUpdateProcessor() {
        return new ItemProcessor<>() {
            private final Map<String, PositionRecord> chunkPositionCache = new HashMap<>();
            @Override
            public PositionRecord process(TransactionRecord transaction) {
            String cacheKey = transaction.getPortfolioId() + "|" + transaction.getInvestmentId();
            PositionRecord position = chunkPositionCache.get(cacheKey);

            if (position == null) {
                List<PositionRecord> existing = positionRepository.findByPortfolioId(transaction.getPortfolioId());
                position = existing.stream()
                        .filter(p -> transaction.getInvestmentId().equals(p.getInvestmentId()))
                        .findFirst()
                        .orElse(null);
            }

            if (position == null) {
                position = new PositionRecord();
                position.setPortfolioId(transaction.getPortfolioId());
                position.setInvestmentId(transaction.getInvestmentId());
                position.setPositionDate(LocalDate.now());
                position.setQuantity(BigDecimal.ZERO);
                position.setCostBasis(BigDecimal.ZERO);
                position.setMarketValue(BigDecimal.ZERO);
                position.setCurrencyCode(transaction.getCurrencyCode());
                position.setStatus(CommonConstants.STATUS_ACTIVE);
            }

            switch (transaction.getTransactionType()) {
                case "BU":
                    position.setQuantity(position.getQuantity().add(transaction.getQuantity()));
                    position.setCostBasis(position.getCostBasis().add(transaction.getAmount()));
                    position.setMarketValue(position.getQuantity().multiply(transaction.getPrice()));
                    break;
                case "SL":
                    position.setQuantity(position.getQuantity().subtract(transaction.getQuantity()));
                    position.setCostBasis(position.getCostBasis().subtract(transaction.getAmount()));
                    position.setMarketValue(position.getQuantity().multiply(transaction.getPrice()));
                    break;
                case "FE":
                    position.setCostBasis(position.getCostBasis().add(transaction.getAmount()));
                    break;
                default:
                    break;
            }
            position.setPositionDate(LocalDate.now());
            position.setLastMaintDate(LocalDateTime.now());
            position.setLastMaintUser("BATCH");
            chunkPositionCache.put(cacheKey, position);
            transaction.setStatus("U");
            transactionRepository.save(transaction);
            return position;
        }
        };
    }

    private ItemWriter<PositionRecord> positionWriter() {
        return chunk -> {
            Map<String, PositionRecord> deduped = new LinkedHashMap<>();
            for (PositionRecord p : chunk.getItems()) {
                String key = p.getPortfolioId() + "|" + p.getInvestmentId();
                deduped.put(key, p);
            }
            for (PositionRecord p : deduped.values()) {
                positionRepository.save(p);
            }
        };
    }

    private ItemProcessor<TransactionRecord, HistoryRecord> historyLoadProcessor() {
        return transaction -> {
            HistoryRecord history = new HistoryRecord();
            history.setPortfolioId(transaction.getPortfolioId());
            history.setHistoryDate(transaction.getTransactionDate().format(DateTimeFormatter.BASIC_ISO_DATE));
            history.setHistoryTime(transaction.getTransactionTime().format(DateTimeFormatter.ofPattern("HHmmss")));
            history.setRecordType("TR");
            history.setActionCode("A");
            history.setAfterImage(String.format("%s|%s|%s|%s",
                    transaction.getTransactionType(),
                    transaction.getQuantity(),
                    transaction.getPrice(),
                    transaction.getAmount()));
            history.setProcessDate(LocalDateTime.now());
            history.setProcessUser("BATCH");
            return history;
        };
    }

    private ItemWriter<HistoryRecord> historyWriter() {
        return chunk -> {
            for (HistoryRecord h : chunk.getItems()) {
                historyRepository.save(h);
            }
            for (TransactionRecord t : transactionRepository.findByStatus("U")) {
                t.setStatus("C");
                transactionRepository.save(t);
            }
        };
    }
}
