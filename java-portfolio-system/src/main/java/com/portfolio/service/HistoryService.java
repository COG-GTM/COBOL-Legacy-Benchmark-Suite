package com.portfolio.service;

import com.portfolio.domain.HistoryRecord;
import com.portfolio.domain.Transaction;
import com.portfolio.repository.HistoryRecordRepository;
import com.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * History Service - migrated from COBOL INQHIST and HISTLD00
 * Handles transaction history inquiries and loading
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final HistoryRecordRepository historyRecordRepository;
    private final TransactionRepository transactionRepository;

    public List<HistoryRecord> getHistoryByPortfolio(String portfolioId) {
        return historyRecordRepository.findHistoryByPortfolio(portfolioId);
    }

    public Page<HistoryRecord> getHistoryByPortfolioPaged(String portfolioId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return historyRecordRepository.findByPortfolioIdOrderByHistoryDateDescHistoryTimeDesc(
                portfolioId, pageable);
    }

    public List<Transaction> getTransactionHistory(String portfolioId) {
        return transactionRepository.findTransactionHistory(portfolioId);
    }

    public Page<Transaction> getTransactionHistoryPaged(String portfolioId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolioId, pageable);
    }

    public List<Transaction> getTransactionsByDateRange(String portfolioId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByPortfolioIdAndTransactionDateBetween(
                portfolioId, startDate, endDate);
    }

    public List<HistoryRecord> getHistoryByDateRange(LocalDate startDate, LocalDate endDate) {
        return historyRecordRepository.findByHistoryDateBetween(startDate, endDate);
    }

    public List<HistoryRecord> getHistoryByUser(String userId) {
        return historyRecordRepository.findByUser(userId);
    }

    @Transactional
    public HistoryRecord createHistoryRecord(HistoryRecord record) {
        HistoryRecord saved = historyRecordRepository.save(record);
        log.info("History record created: portfolio={}, type={}, action={}",
                record.getPortfolioId(), record.getRecordType(), record.getActionCode());
        return saved;
    }

    public HistoryLoadResult loadHistoryFromTransactions(LocalDate processDate) {
        List<Transaction> completedTransactions = transactionRepository
                .findByTransactionDateBetween(processDate, processDate);
        
        int loadedCount = 0;
        int errorCount = 0;
        
        for (Transaction transaction : completedTransactions) {
            if (transaction.getStatus() == Transaction.TransactionStatus.D) {
                try {
                    HistoryRecord history = HistoryRecord.builder()
                            .portfolioId(transaction.getPortfolioId())
                            .historyDate(transaction.getTransactionDate())
                            .historyTime(transaction.getTransactionTime())
                            .recordType(HistoryRecord.RecordType.TR)
                            .actionCode(HistoryRecord.ActionCode.A)
                            .afterImage(serializeTransaction(transaction))
                            .processUser(transaction.getProcessUser())
                            .build();
                    
                    historyRecordRepository.save(history);
                    loadedCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("Error loading history for transaction: {}", transaction.getId(), e);
                }
            }
        }
        
        HistoryLoadResult result = new HistoryLoadResult();
        result.setTotalProcessed(completedTransactions.size());
        result.setLoadedCount(loadedCount);
        result.setErrorCount(errorCount);
        result.setReturnCode(errorCount > 0 ? 8 : 0);
        
        log.info("History load complete: processed={}, loaded={}, errors={}",
                completedTransactions.size(), loadedCount, errorCount);
        
        return result;
    }

    private String serializeTransaction(Transaction transaction) {
        return String.format("TYPE:%s,AMT:%s,QTY:%s,INV:%s",
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getQuantity(),
                transaction.getInvestmentId());
    }

    public static class HistoryLoadResult {
        private int totalProcessed;
        private int loadedCount;
        private int errorCount;
        private int returnCode;

        public int getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
        public int getLoadedCount() { return loadedCount; }
        public void setLoadedCount(int loadedCount) { this.loadedCount = loadedCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public int getReturnCode() { return returnCode; }
        public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    }
}
