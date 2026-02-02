package com.portfolio.service;

import com.portfolio.domain.*;
import com.portfolio.exception.PortfolioException;
import com.portfolio.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Position Update Service - migrated from COBOL POSUPD00
 * Updates portfolio positions based on validated transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PositionUpdateService {

    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final HistoryRecordRepository historyRecordRepository;

    /**
     * Process a validated transaction and update positions
     * Equivalent to COBOL 2000-PROCESS-TRANSACTION
     */
    @Transactional
    public UpdateResult processTransaction(Transaction transaction) {
        UpdateResult result = new UpdateResult();
        
        try {
            switch (transaction.getTransactionType()) {
                case BU -> processBuyTransaction(transaction, result);
                case SL -> processSellTransaction(transaction, result);
                case TR -> processTransferTransaction(transaction, result);
                case FE -> processFeeTransaction(transaction, result);
            }
            
            transaction.setStatus(Transaction.TransactionStatus.D);
            transaction.setProcessDate(LocalDateTime.now());
            transactionRepository.save(transaction);
            
            updatePortfolioTotals(transaction.getPortfolioId());
            
            result.setSuccess(true);
            result.setReturnCode(0);
            log.info("Transaction processed successfully: id={}, type={}", 
                    transaction.getId(), transaction.getTransactionType());
            
        } catch (Exception e) {
            transaction.setStatus(Transaction.TransactionStatus.F);
            transactionRepository.save(transaction);
            
            result.setSuccess(false);
            result.setReturnCode(8);
            result.setErrorMessage(e.getMessage());
            log.error("Transaction processing failed: id={}, error={}", 
                    transaction.getId(), e.getMessage());
        }
        
        return result;
    }

    /**
     * Process all pending transactions
     * Equivalent to COBOL batch processing loop
     */
    @Transactional
    public BatchUpdateResult processPendingTransactions() {
        List<Transaction> pendingTransactions = transactionRepository.findPendingTransactions();
        
        BatchUpdateResult batchResult = new BatchUpdateResult();
        int successCount = 0;
        int errorCount = 0;
        
        for (Transaction transaction : pendingTransactions) {
            UpdateResult result = processTransaction(transaction);
            if (result.isSuccess()) {
                successCount++;
            } else {
                errorCount++;
            }
        }
        
        batchResult.setTotalProcessed(pendingTransactions.size());
        batchResult.setSuccessCount(successCount);
        batchResult.setErrorCount(errorCount);
        batchResult.setReturnCode(errorCount > 0 ? 8 : 0);
        
        log.info("Batch position update complete: total={}, success={}, errors={}",
                pendingTransactions.size(), successCount, errorCount);
        
        return batchResult;
    }

    private void processBuyTransaction(Transaction transaction, UpdateResult result) {
        PositionId positionId = new PositionId(
                transaction.getPortfolioId(),
                LocalDate.now(),
                transaction.getInvestmentId()
        );
        
        Position position = positionRepository.findById(positionId)
                .orElse(createNewPosition(transaction));
        
        String beforeImage = serializePosition(position);
        
        BigDecimal newQuantity = position.getQuantity().add(transaction.getQuantity());
        BigDecimal newCostBasis = position.getCostBasis().add(transaction.getAmount());
        
        position.setQuantity(newQuantity);
        position.setCostBasis(newCostBasis);
        position.setStatus(Position.PositionStatus.A);
        position.setLastMaintUser(transaction.getProcessUser());
        
        positionRepository.save(position);
        
        String afterImage = serializePosition(position);
        createHistoryRecord(transaction.getPortfolioId(), HistoryRecord.RecordType.PS,
                HistoryRecord.ActionCode.C, beforeImage, afterImage, transaction.getProcessUser());
        
        result.setPositionUpdated(true);
        result.setNewQuantity(newQuantity);
        result.setNewCostBasis(newCostBasis);
    }

    private void processSellTransaction(Transaction transaction, UpdateResult result) {
        List<Position> positions = positionRepository.findActivePositionsByPortfolio(
                transaction.getPortfolioId());
        
        Position position = positions.stream()
                .filter(p -> p.getInvestmentId().equals(transaction.getInvestmentId()))
                .findFirst()
                .orElseThrow(() -> new PortfolioException(
                        "Position not found for sell", 
                        PortfolioException.ErrorCode.NOT_FOUND));
        
        String beforeImage = serializePosition(position);
        
        BigDecimal newQuantity = position.getQuantity().subtract(transaction.getQuantity());
        BigDecimal costPerUnit = position.getCostBasis()
                .divide(position.getQuantity(), 4, RoundingMode.HALF_UP);
        BigDecimal costReduction = costPerUnit.multiply(transaction.getQuantity());
        BigDecimal newCostBasis = position.getCostBasis().subtract(costReduction);
        
        position.setQuantity(newQuantity);
        position.setCostBasis(newCostBasis);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            position.setStatus(Position.PositionStatus.C);
        }
        
        position.setLastMaintUser(transaction.getProcessUser());
        positionRepository.save(position);
        
        String afterImage = serializePosition(position);
        createHistoryRecord(transaction.getPortfolioId(), HistoryRecord.RecordType.PS,
                HistoryRecord.ActionCode.C, beforeImage, afterImage, transaction.getProcessUser());
        
        result.setPositionUpdated(true);
        result.setNewQuantity(newQuantity);
        result.setNewCostBasis(newCostBasis);
        result.setGainLoss(transaction.getAmount().subtract(costReduction));
    }

    private void processTransferTransaction(Transaction transaction, UpdateResult result) {
        result.setPositionUpdated(false);
        result.setMessage("Transfer transaction logged");
    }

    private void processFeeTransaction(Transaction transaction, UpdateResult result) {
        Portfolio portfolio = portfolioRepository.findById(transaction.getPortfolioId())
                .orElseThrow(() -> new PortfolioException(
                        "Portfolio not found", 
                        PortfolioException.ErrorCode.NOT_FOUND));
        
        BigDecimal newCashBalance = portfolio.getCashBalance().subtract(transaction.getAmount());
        portfolio.setCashBalance(newCashBalance);
        portfolioRepository.save(portfolio);
        
        result.setPositionUpdated(false);
        result.setMessage("Fee deducted from cash balance");
    }

    private Position createNewPosition(Transaction transaction) {
        return Position.builder()
                .portfolioId(transaction.getPortfolioId())
                .positionDate(LocalDate.now())
                .investmentId(transaction.getInvestmentId())
                .quantity(BigDecimal.ZERO)
                .costBasis(BigDecimal.ZERO)
                .marketValue(BigDecimal.ZERO)
                .currency(transaction.getCurrency())
                .status(Position.PositionStatus.A)
                .build();
    }

    private void updatePortfolioTotals(String portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
        if (portfolio != null) {
            BigDecimal totalMarketValue = positionRepository.calculateTotalMarketValue(portfolioId);
            BigDecimal totalCostBasis = positionRepository.calculateTotalCostBasis(portfolioId);
            
            portfolio.setTotalValue(totalMarketValue != null ? totalMarketValue : BigDecimal.ZERO);
            portfolio.setLastTransDate(LocalDate.now());
            portfolioRepository.save(portfolio);
        }
    }

    private void createHistoryRecord(String portfolioId, HistoryRecord.RecordType recordType,
                                     HistoryRecord.ActionCode actionCode, String beforeImage,
                                     String afterImage, String userId) {
        HistoryRecord history = HistoryRecord.builder()
                .portfolioId(portfolioId)
                .historyDate(LocalDate.now())
                .recordType(recordType)
                .actionCode(actionCode)
                .beforeImage(beforeImage)
                .afterImage(afterImage)
                .processUser(userId)
                .processDate(LocalDateTime.now())
                .build();
        historyRecordRepository.save(history);
    }

    private String serializePosition(Position position) {
        return String.format("QTY:%s,COST:%s,STATUS:%s",
                position.getQuantity(), position.getCostBasis(), position.getStatus());
    }

    public static class UpdateResult {
        private boolean success;
        private boolean positionUpdated;
        private BigDecimal newQuantity;
        private BigDecimal newCostBasis;
        private BigDecimal gainLoss;
        private String message;
        private String errorMessage;
        private int returnCode;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public boolean isPositionUpdated() { return positionUpdated; }
        public void setPositionUpdated(boolean positionUpdated) { this.positionUpdated = positionUpdated; }
        public BigDecimal getNewQuantity() { return newQuantity; }
        public void setNewQuantity(BigDecimal newQuantity) { this.newQuantity = newQuantity; }
        public BigDecimal getNewCostBasis() { return newCostBasis; }
        public void setNewCostBasis(BigDecimal newCostBasis) { this.newCostBasis = newCostBasis; }
        public BigDecimal getGainLoss() { return gainLoss; }
        public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getReturnCode() { return returnCode; }
        public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    }

    public static class BatchUpdateResult {
        private int totalProcessed;
        private int successCount;
        private int errorCount;
        private int returnCode;

        public int getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public int getReturnCode() { return returnCode; }
        public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    }
}
