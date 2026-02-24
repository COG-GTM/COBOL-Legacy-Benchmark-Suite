package com.portfolio.batch;

import com.portfolio.entity.PositionHistory;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.repository.PositionHistoryRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * History Load Step - replaces HISTLD00.cbl.
 * Source: src/programs/batch/HISTLD00.cbl
 *
 * COBOL logic:
 * - P200-LOAD-HISTORY: Read validated transactions from sequential file
 * - P210-FORMAT-HISTORY: Format history record from transaction fields
 * - P220-INSERT-DB2: INSERT INTO POSHIST (DB2 table)
 * - Uses EXEC SQL INSERT with host variables mapped from WS-HISTORY-REC
 */
@Component
public class HistoryLoadStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadStep.class);

    private final TransactionHistoryRepository transactionRepository;
    private final PositionHistoryRepository historyRepository;

    public HistoryLoadStep(TransactionHistoryRepository transactionRepository,
                           PositionHistoryRepository historyRepository) {
        this.transactionRepository = transactionRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("HISTLD00: Starting history load to DB2 (position_history)");

        List<TransactionHistory> processedTransactions = transactionRepository.findByStatus("P");
        int loadCount = 0;

        for (TransactionHistory txn : processedTransactions) {
            PositionHistory history = mapToHistory(txn);
            historyRepository.save(history);
            loadCount++;
        }

        log.info("HISTLD00: Completed. Records loaded={}", loadCount);

        return RepeatStatus.FINISHED;
    }

    /**
     * Map transaction to position history record - replaces P210-FORMAT-HISTORY.
     */
    private PositionHistory mapToHistory(TransactionHistory txn) {
        PositionHistory history = new PositionHistory();
        history.setPortfolioId(txn.getPortfolioId());
        history.setTransDate(txn.getTransactionDate());
        history.setTransTime(txn.getTransactionTime());
        history.setTransType(txn.getTransactionType());
        history.setSecurityId(txn.getInvestmentId());
        history.setQuantity(txn.getQuantity());
        history.setPrice(txn.getPrice());
        history.setAmount(txn.getAmount());
        history.setProcessDate(LocalDate.now());
        history.setProcessTime(LocalTime.now());
        history.setProgramId("HISTLD00");
        history.setUserId("BATCH");
        history.setAuditTimestamp(LocalDateTime.now());
        return history;
    }
}
