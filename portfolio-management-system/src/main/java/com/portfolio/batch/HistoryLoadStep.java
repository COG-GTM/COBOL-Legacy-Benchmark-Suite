package com.portfolio.batch;

import com.portfolio.model.HistoryRecord;
import com.portfolio.model.TransactionRecord;
import com.portfolio.support.BatchExceptions;
import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.ErrorLoggingService;
import com.portfolio.support.HistoryRecordRepository;
import com.portfolio.support.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * History Load Step.
 * Migrated from COBOL HISTLD00.
 * Reads TRANSACTION_HISTORY_VSAM, transforms to DB2 analytical format,
 * bulk inserts to TRANSACTION_HISTORY (DB2).
 * Maps exit status to RC 0/4/8/12.
 */
@Component
public class HistoryLoadStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadStep.class);

    private final HistoryRecordRepository historyRepository;
    private final TransactionRecordRepository transactionRepository;
    private final ErrorLoggingService errorLoggingService;
    private final Db2StatisticsService statisticsService;

    private int recordsProcessed = 0;
    private int recordsError = 0;
    private int recordsWarning = 0;

    public HistoryLoadStep(
            HistoryRecordRepository historyRepository,
            TransactionRecordRepository transactionRepository,
            ErrorLoggingService errorLoggingService,
            Db2StatisticsService statisticsService) {
        this.historyRepository = historyRepository;
        this.transactionRepository = transactionRepository;
        this.errorLoggingService = errorLoggingService;
        this.statisticsService = statisticsService;
    }

    // Unique sequence counter for transaction ID generation within a single execution
    private final AtomicInteger txnIdSequence = new AtomicInteger(0);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Starting History Load Step (HISTLD00)");

        // Reset counters for each execution (singleton component reused across runs)
        recordsProcessed = 0;
        recordsError = 0;
        recordsWarning = 0;
        txnIdSequence.set(0);

        List<HistoryRecord> historyRecords = historyRepository.findAll();
        log.info("Found {} history records to load to DB2", historyRecords.size());

        for (HistoryRecord history : historyRecords) {
            try {
                TransactionRecord txnRecord = transformToDb2Format(history);
                transactionRepository.save(txnRecord);
                recordsProcessed++;
                statisticsService.recordInsert();
            } catch (BatchExceptions.BatchWarningException e) {
                recordsWarning++;
                errorLoggingService.logWarning("HISTLD00", "HST4",
                        e.getMessage(), "BATCH");
            } catch (Exception e) {
                recordsError++;
                errorLoggingService.logApplicationError("HISTLD00", "HST8",
                        "History load failed: " + e.getMessage(),
                        history.getPortfolioId(), "BATCH");
            }
        }

        int returnCode = determineReturnCode();
        contribution.setExitStatus(new ExitStatus("RC_" + returnCode));

        log.info("History Load complete: processed={}, warnings={}, errors={}, RC={}",
                recordsProcessed, recordsWarning, recordsError, returnCode);

        return RepeatStatus.FINISHED;
    }

    /**
     * Transform VSAM history record to DB2 analytical format.
     */
    private TransactionRecord transformToDb2Format(HistoryRecord history) {
        TransactionRecord txn = new TransactionRecord();

        // Generate unique transaction ID: YYYYMMDD + portfolio hash + 6-digit sequence
        // Uses an independent AtomicInteger counter to guarantee uniqueness across all records
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seqStr = String.format("%06d", txnIdSequence.incrementAndGet());
        txn.setTransactionId(dateStr + history.getPortfolioId().hashCode() + seqStr);

        txn.setPortfolioId(history.getPortfolioId());
        txn.setTransactionDate(history.getTxnDate());
        txn.setTransactionTime(LocalTime.now());
        txn.setInvestmentId(history.getInvestmentId());
        txn.setTransactionType(history.getTransactionType());
        txn.setQuantity(history.getQuantity());
        txn.setPrice(history.getPrice());
        txn.setAmount(history.getAmount());
        txn.setCurrencyCode(history.getCurrencyCode());
        txn.setStatus(TransactionRecord.STATUS_DONE);
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("BATCH");

        return txn;
    }

    private int determineReturnCode() {
        if (recordsError > 0) return BatchExceptions.RC_ERROR;
        if (recordsWarning > 0) return BatchExceptions.RC_WARNING;
        return BatchExceptions.RC_SUCCESS;
    }

    public int getRecordsProcessed() { return recordsProcessed; }
    public int getRecordsError() { return recordsError; }
}
