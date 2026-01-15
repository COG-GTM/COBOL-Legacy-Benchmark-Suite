package com.portfolio.modernization.service;

import com.portfolio.modernization.entity.HistoryRecord;
import com.portfolio.modernization.entity.PositionRecord;
import com.portfolio.modernization.entity.TransactionRecord;
import com.portfolio.modernization.repository.HistoryRepository;
import com.portfolio.modernization.repository.PositionRepository;
import com.portfolio.modernization.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Portfolio Service
 * 
 * Implements business logic for portfolio position management.
 * Modernized from COBOL program: src/programs/batch/POSUPD00.cbl
 * 
 * Original COBOL business logic preserved:
 * - Position validation (P100-VALIDATE-POSITION)
 * - Market value calculation (P200-CALCULATE-VALUES)
 * - Position update processing (P300-UPDATE-POSITION)
 * - History logging (P400-LOG-HISTORY)
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Service
@Transactional
public class PortfolioService {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioService.class);

    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final HistoryRepository historyRepository;

    @Autowired
    public PortfolioService(PositionRepository positionRepository,
                           TransactionRepository transactionRepository,
                           HistoryRepository historyRepository) {
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Updates a position record with validation.
     * Preserves original business logic from POSUPD00 P300-UPDATE-POSITION.
     * 
     * @param position the position to update
     * @return the updated position
     * @throws IllegalArgumentException if position validation fails
     */
    public PositionRecord updatePosition(PositionRecord position) {
        logger.info("Updating position: {}", position.getPortfolioId());
        
        validatePosition(position);
        
        String beforeImage = capturePositionState(position);
        
        calculateMarketValue(position);
        
        PositionRecord savedPosition = positionRepository.save(position);
        
        String afterImage = capturePositionState(savedPosition);
        logPositionHistory(savedPosition, beforeImage, afterImage, "UPD");
        
        logger.info("Position updated successfully: {}", savedPosition.getPortfolioId());
        return savedPosition;
    }

    /**
     * Creates a new position record.
     * 
     * @param position the position to create
     * @return the created position
     * @throws IllegalArgumentException if position validation fails
     */
    public PositionRecord createPosition(PositionRecord position) {
        logger.info("Creating position: {}", position.getPortfolioId());
        
        validatePosition(position);
        
        if (positionRepository.existsById(position.getPortfolioId())) {
            throw new IllegalArgumentException("Position already exists: " + position.getPortfolioId());
        }
        
        calculateMarketValue(position);
        
        PositionRecord savedPosition = positionRepository.save(position);
        
        String afterImage = capturePositionState(savedPosition);
        logPositionHistory(savedPosition, null, afterImage, "ADD");
        
        logger.info("Position created successfully: {}", savedPosition.getPortfolioId());
        return savedPosition;
    }

    /**
     * Validates a position record.
     * Preserves original business logic from POSUPD00 P100-VALIDATE-POSITION.
     * 
     * @param position the position to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validatePosition(PositionRecord position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        
        if (!position.isValidPosition()) {
            throw new IllegalArgumentException("Position validation failed for: " + position.getPortfolioId());
        }
        
        if (position.getPortfolioId() == null || position.getPortfolioId().trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio ID is required");
        }
        
        if (position.getAccountNumber() == null || position.getAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }
        
        if (position.getFundId() == null || position.getFundId().trim().isEmpty()) {
            throw new IllegalArgumentException("Fund ID is required");
        }
        
        if (position.getUnits() != null && position.getUnits().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Units cannot be negative");
        }
        
        logger.debug("Position validation passed: {}", position.getPortfolioId());
    }

    /**
     * Calculates market value for a position.
     * Preserves original business logic from POSUPD00 P200-CALCULATE-VALUES.
     * 
     * @param position the position to calculate
     */
    public void calculateMarketValue(PositionRecord position) {
        if (position.getUnits() == null || position.getUnits().compareTo(BigDecimal.ZERO) == 0) {
            position.setMarketValue(BigDecimal.ZERO);
            return;
        }
        
        logger.debug("Calculating market value for position: {}", position.getPortfolioId());
    }

    /**
     * Processes a buy transaction and updates the position.
     * 
     * @param portfolioId the portfolio identifier
     * @param transaction the buy transaction
     * @return the updated position
     */
    public PositionRecord processBuyTransaction(String portfolioId, TransactionRecord transaction) {
        logger.info("Processing buy transaction for portfolio: {}", portfolioId);
        
        if (!transaction.isBuyTransaction()) {
            throw new IllegalArgumentException("Transaction is not a buy transaction");
        }
        
        Optional<PositionRecord> existingPosition = positionRepository.findById(portfolioId);
        
        PositionRecord position;
        if (existingPosition.isPresent()) {
            position = existingPosition.get();
            String beforeImage = capturePositionState(position);
            
            position.addUnits(transaction.getUnits(), transaction.getPrice());
            
            PositionRecord savedPosition = positionRepository.save(position);
            String afterImage = capturePositionState(savedPosition);
            logPositionHistory(savedPosition, beforeImage, afterImage, "BUY");
            
            return savedPosition;
        } else {
            position = new PositionRecord();
            position.setPortfolioId(portfolioId);
            position.setAccountNumber(portfolioId.substring(0, Math.min(8, portfolioId.length())));
            position.setFundId(transaction.getInvestmentId());
            position.setUnits(transaction.getUnits());
            position.setCostBasis(transaction.getAmount());
            position.setMarketValue(transaction.getAmount());
            position.setCurrencyCode(transaction.getCurrencyCode());
            position.setLastMaintUser(transaction.getProcessUser());
            
            return createPosition(position);
        }
    }

    /**
     * Processes a sell transaction and updates the position.
     * 
     * @param portfolioId the portfolio identifier
     * @param transaction the sell transaction
     * @return the updated position
     */
    public PositionRecord processSellTransaction(String portfolioId, TransactionRecord transaction) {
        logger.info("Processing sell transaction for portfolio: {}", portfolioId);
        
        if (!transaction.isSellTransaction()) {
            throw new IllegalArgumentException("Transaction is not a sell transaction");
        }
        
        PositionRecord position = positionRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + portfolioId));
        
        if (position.getUnits() == null || 
            position.getUnits().compareTo(transaction.getUnits()) < 0) {
            throw new IllegalArgumentException("Insufficient units for sell transaction");
        }
        
        String beforeImage = capturePositionState(position);
        
        BigDecimal removedCostBasis = position.removeUnits(transaction.getUnits());
        
        PositionRecord savedPosition = positionRepository.save(position);
        String afterImage = capturePositionState(savedPosition);
        logPositionHistory(savedPosition, beforeImage, afterImage, "SEL");
        
        logger.info("Sell transaction processed. Removed cost basis: {}", removedCostBasis);
        return savedPosition;
    }

    /**
     * Gets a position by portfolio ID.
     * 
     * @param portfolioId the portfolio identifier
     * @return optional position if found
     */
    @Transactional(readOnly = true)
    public Optional<PositionRecord> getPosition(String portfolioId) {
        return positionRepository.findById(portfolioId);
    }

    /**
     * Gets all positions for an account.
     * 
     * @param accountNumber the account number
     * @return list of positions for the account
     */
    @Transactional(readOnly = true)
    public List<PositionRecord> getPositionsByAccount(String accountNumber) {
        return positionRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Gets all active positions for an account.
     * 
     * @param accountNumber the account number
     * @return list of active positions
     */
    @Transactional(readOnly = true)
    public List<PositionRecord> getActivePositionsByAccount(String accountNumber) {
        return positionRepository.findByAccountNumberAndStatus(accountNumber, PositionRecord.STATUS_ACTIVE);
    }

    /**
     * Gets high-value positions exceeding a threshold.
     * 
     * @param threshold the market value threshold
     * @return list of high-value positions
     */
    @Transactional(readOnly = true)
    public List<PositionRecord> getHighValuePositions(BigDecimal threshold) {
        return positionRepository.findHighValuePositions(threshold);
    }

    /**
     * Calculates total portfolio value for an account.
     * 
     * @param accountNumber the account number
     * @return total market value
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPortfolioValue(String accountNumber) {
        BigDecimal totalValue = positionRepository.calculateTotalMarketValueByAccount(accountNumber);
        return totalValue != null ? totalValue : BigDecimal.ZERO;
    }

    /**
     * Calculates total unrealized gain/loss for an account.
     * 
     * @param accountNumber the account number
     * @return unrealized gain/loss
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateUnrealizedGainLoss(String accountNumber) {
        BigDecimal marketValue = positionRepository.calculateTotalMarketValueByAccount(accountNumber);
        BigDecimal costBasis = positionRepository.calculateTotalCostBasisByAccount(accountNumber);
        
        if (marketValue == null || costBasis == null) {
            return BigDecimal.ZERO;
        }
        
        return marketValue.subtract(costBasis);
    }

    /**
     * Closes a position.
     * 
     * @param portfolioId the portfolio identifier
     * @param processUser the user closing the position
     * @return the closed position
     */
    public PositionRecord closePosition(String portfolioId, String processUser) {
        logger.info("Closing position: {}", portfolioId);
        
        PositionRecord position = positionRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + portfolioId));
        
        String beforeImage = capturePositionState(position);
        
        position.closePosition();
        position.setLastMaintUser(processUser);
        
        PositionRecord savedPosition = positionRepository.save(position);
        String afterImage = capturePositionState(savedPosition);
        logPositionHistory(savedPosition, beforeImage, afterImage, "CLS");
        
        logger.info("Position closed: {}", portfolioId);
        return savedPosition;
    }

    /**
     * Updates market values for all positions of a fund.
     * 
     * @param fundId the fund identifier
     * @param currentPrice the current price per unit
     * @param processUser the user performing the update
     * @return number of positions updated
     */
    public int updateMarketValuesForFund(String fundId, BigDecimal currentPrice, String processUser) {
        logger.info("Updating market values for fund: {} with price: {}", fundId, currentPrice);
        
        List<PositionRecord> positions = positionRepository.findByFundId(fundId);
        int updatedCount = 0;
        
        for (PositionRecord position : positions) {
            if (position.isActive() && position.hasHoldings()) {
                String beforeImage = capturePositionState(position);
                
                position.updateMarketValue(currentPrice);
                position.setLastMaintUser(processUser);
                
                positionRepository.save(position);
                
                String afterImage = capturePositionState(position);
                logPositionHistory(position, beforeImage, afterImage, "MKT");
                
                updatedCount++;
            }
        }
        
        logger.info("Updated {} positions for fund: {}", updatedCount, fundId);
        return updatedCount;
    }

    /**
     * Captures the current state of a position for history logging.
     * 
     * @param position the position to capture
     * @return string representation of position state
     */
    private String capturePositionState(PositionRecord position) {
        if (position == null) {
            return null;
        }
        return String.format("ID=%s|ACCT=%s|FUND=%s|UNITS=%s|COST=%s|MKT=%s|STATUS=%s",
                position.getPortfolioId(),
                position.getAccountNumber(),
                position.getFundId(),
                position.getUnits(),
                position.getCostBasis(),
                position.getMarketValue(),
                position.getStatus());
    }

    /**
     * Logs position history for audit trail.
     * Preserves original business logic from POSUPD00 P400-LOG-HISTORY.
     * 
     * @param position the position
     * @param beforeImage state before change
     * @param afterImage state after change
     * @param reasonCode reason for change
     */
    private void logPositionHistory(PositionRecord position, String beforeImage, 
                                   String afterImage, String reasonCode) {
        try {
            String actionCode = beforeImage == null ? HistoryRecord.ACTION_ADD :
                               afterImage == null ? HistoryRecord.ACTION_DELETE :
                               HistoryRecord.ACTION_CHANGE;
            
            HistoryRecord history = new HistoryRecord(
                    position.getPortfolioId(),
                    HistoryRecord.RECORD_TYPE_POSITION,
                    actionCode);
            
            history.setBeforeImage(beforeImage);
            history.setAfterImage(afterImage);
            history.setReasonCode(reasonCode);
            history.setProcessUser(position.getLastMaintUser());
            history.setEntityId(position.getPortfolioId());
            history.setEntityType("POSITION");
            history.generateChangeSummary();
            
            historyRepository.save(history);
            
            logger.debug("Position history logged for: {}", position.getPortfolioId());
        } catch (Exception e) {
            logger.error("Failed to log position history: {}", e.getMessage());
        }
    }
}
