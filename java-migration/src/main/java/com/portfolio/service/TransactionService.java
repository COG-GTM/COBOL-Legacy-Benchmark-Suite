package com.portfolio.service;

import com.portfolio.config.RedisConfig;
import com.portfolio.entity.TransactionRecord;
import com.portfolio.repository.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for Transaction Record operations
 * Implements caching for frequently accessed data
 */
@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRecordRepository transactionRepository;

    public TransactionService(TransactionRecordRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Find transaction by ID with caching
     */
    @Cacheable(value = RedisConfig.TRANSACTION_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public Optional<TransactionRecord> findById(UUID id) {
        logger.debug("Finding transaction by ID: {}", id);
        return transactionRepository.findById(id);
    }

    /**
     * Find transaction by transaction ID with caching
     */
    @Cacheable(value = RedisConfig.TRANSACTION_CACHE, key = "'tid-' + #transactionId")
    @Transactional(readOnly = true)
    public Optional<TransactionRecord> findByTransactionId(String transactionId) {
        logger.debug("Finding transaction by transaction ID: {}", transactionId);
        return transactionRepository.findByTransactionId(transactionId);
    }

    /**
     * Find all transactions by portfolio ID
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> findByPortfolioId(String portfolioId) {
        logger.debug("Finding transactions by portfolio ID: {}", portfolioId);
        return transactionRepository.findByPortfolioId(portfolioId);
    }

    /**
     * Find transactions by portfolio ID with pagination
     */
    @Transactional(readOnly = true)
    public Page<TransactionRecord> findByPortfolioId(String portfolioId, Pageable pageable) {
        logger.debug("Finding transactions by portfolio ID with pagination: {}", portfolioId);
        return transactionRepository.findByPortfolioId(portfolioId, pageable);
    }

    /**
     * Find transactions by portfolio ID within date range
     */
    @Cacheable(value = RedisConfig.TRANSACTION_CACHE, 
               key = "'range-' + #portfolioId + '-' + #startDate + '-' + #endDate")
    @Transactional(readOnly = true)
    public List<TransactionRecord> findByPortfolioIdAndDateRange(
            String portfolioId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Finding transactions for portfolio {} between {} and {}", 
                portfolioId, startDate, endDate);
        return transactionRepository.findByPortfolioIdAndTransactionDateBetween(
                portfolioId, startDate, endDate);
    }

    /**
     * Find transactions by portfolio ID within date range with pagination
     */
    @Transactional(readOnly = true)
    public Page<TransactionRecord> findByPortfolioIdAndDateRange(
            String portfolioId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        logger.debug("Finding transactions for portfolio {} between {} and {} with pagination", 
                portfolioId, startDate, endDate);
        return transactionRepository.findByPortfolioIdAndTransactionDateBetween(
                portfolioId, startDate, endDate, pageable);
    }

    /**
     * Find pending transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> findPendingTransactions() {
        logger.debug("Finding pending transactions");
        return transactionRepository.findPendingTransactions();
    }

    /**
     * Find recent transactions (last 30 days)
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> findRecentTransactions() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        logger.debug("Finding recent transactions since: {}", startDate);
        return transactionRepository.findRecentTransactions(startDate);
    }

    /**
     * Find recent transactions with pagination
     */
    @Transactional(readOnly = true)
    public Page<TransactionRecord> findRecentTransactions(Pageable pageable) {
        LocalDate startDate = LocalDate.now().minusDays(30);
        logger.debug("Finding recent transactions since: {} with pagination", startDate);
        return transactionRepository.findRecentTransactions(startDate, pageable);
    }

    /**
     * Find buy transactions by portfolio ID
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> findBuyTransactions(String portfolioId) {
        logger.debug("Finding buy transactions for portfolio: {}", portfolioId);
        return transactionRepository.findBuyTransactions(portfolioId);
    }

    /**
     * Find sell transactions by portfolio ID
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> findSellTransactions(String portfolioId) {
        logger.debug("Finding sell transactions for portfolio: {}", portfolioId);
        return transactionRepository.findSellTransactions(portfolioId);
    }

    /**
     * Save transaction with cache eviction
     */
    @CacheEvict(value = RedisConfig.TRANSACTION_CACHE, allEntries = true)
    public TransactionRecord save(TransactionRecord transaction) {
        logger.debug("Saving transaction: {}", transaction.getTransactionId());
        return transactionRepository.save(transaction);
    }

    /**
     * Update transaction with cache eviction
     */
    @CacheEvict(value = RedisConfig.TRANSACTION_CACHE, allEntries = true)
    public TransactionRecord update(TransactionRecord transaction) {
        logger.debug("Updating transaction: {}", transaction.getTransactionId());
        return transactionRepository.save(transaction);
    }

    /**
     * Delete transaction with cache eviction
     */
    @CacheEvict(value = RedisConfig.TRANSACTION_CACHE, allEntries = true)
    public void delete(UUID id) {
        logger.debug("Deleting transaction: {}", id);
        transactionRepository.deleteById(id);
    }

    /**
     * Get total amount by portfolio ID and transaction type
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalAmountByPortfolioIdAndType(String portfolioId, String type) {
        logger.debug("Getting total amount for portfolio {} and type {}", portfolioId, type);
        BigDecimal total = transactionRepository.getTotalAmountByPortfolioIdAndType(portfolioId, type);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Count transactions by portfolio ID
     */
    @Transactional(readOnly = true)
    public long countByPortfolioId(String portfolioId) {
        return transactionRepository.countByPortfolioId(portfolioId);
    }

    /**
     * Count transactions by status
     */
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return transactionRepository.countByStatus(status);
    }
}
