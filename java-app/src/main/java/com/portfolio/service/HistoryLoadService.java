package com.portfolio.service;

import com.portfolio.model.PositionHistory;
import com.portfolio.model.TransactionHistory;
import com.portfolio.repository.PositionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * History Load Service.
 * Replaces: HISTLD00.cbl - Loads processed transactions into history tables.
 *
 * Handles the DB2 INSERT operations that the COBOL program does via embedded SQL.
 * Implements commit-interval logic (replaces HISTLD00 2300-CHECK-COMMIT paragraph).
 */
@Service
public class HistoryLoadService {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadService.class);

    private final PositionHistoryRepository positionHistoryRepository;
    private final AtomicInteger sequenceCounter = new AtomicInteger(0);

    public HistoryLoadService(PositionHistoryRepository positionHistoryRepository) {
        this.positionHistoryRepository = positionHistoryRepository;
    }

    /**
     * Loads a processed transaction into the position history table.
     * Replaces: HISTLD00.cbl 2200-LOAD-TO-DB2 paragraph.
     */
    @Transactional
    public PositionHistory loadTransactionToHistory(TransactionHistory transaction, String userId) {
        PositionHistory history = new PositionHistory();

        history.setPortfolioId(transaction.getPortfolioId());
        history.setHistoryDate(transaction.getTransactionDate());
        history.setHistoryTime(transaction.getTransactionTime() != null
                ? transaction.getTransactionTime() : LocalTime.now());
        history.setSequenceNo(String.format("%04d", sequenceCounter.incrementAndGet() % 10000));
        history.setRecordType("TR");
        history.setActionCode("A");
        history.setInvestmentId(transaction.getInvestmentId());
        history.setQuantity(transaction.getQuantity());
        history.setCostBasis(transaction.getAmount());
        history.setMarketValue(transaction.getQuantity().multiply(transaction.getPrice()));
        history.setReasonCode(transaction.getTransactionType());
        history.setProcessDate(LocalDateTime.now());
        history.setProcessUser(userId);

        log.debug("Loading history for transaction {} portfolio {}",
                transaction.getTransactionId(), transaction.getPortfolioId());

        return positionHistoryRepository.save(history);
    }

    /**
     * Resets the sequence counter.
     * Used at the start of a batch run.
     */
    public void resetSequence() {
        sequenceCounter.set(0);
    }

    /**
     * Gets current processing statistics.
     * Replaces: HISTLD00.cbl 3400-DISPLAY-STATS paragraph.
     */
    public int getProcessedCount() {
        return sequenceCounter.get();
    }
}
