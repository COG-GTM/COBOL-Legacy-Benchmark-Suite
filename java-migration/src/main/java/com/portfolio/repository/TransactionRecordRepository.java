package com.portfolio.repository;

import com.portfolio.entity.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Transaction Record entity
 * Provides data access methods for transaction operations
 * Supports row-level locking to replicate VSAM record-level locking behavior
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, UUID> {

    /**
     * Find transaction by composite key
     * Replicates VSAM KSDS key access pattern
     */
    Optional<TransactionRecord> findByTransactionDateAndTransactionTimeAndPortfolioIdAndSequenceNo(
            LocalDate transactionDate, LocalTime transactionTime, String portfolioId, String sequenceNo);

    /**
     * Find transaction by composite key with pessimistic lock
     * Replicates VSAM record-level locking for updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransactionRecord t WHERE t.transactionDate = :transactionDate " +
            "AND t.transactionTime = :transactionTime AND t.portfolioId = :portfolioId " +
            "AND t.sequenceNo = :sequenceNo")
    Optional<TransactionRecord> findByKeyWithLock(
            @Param("transactionDate") LocalDate transactionDate,
            @Param("transactionTime") LocalTime transactionTime,
            @Param("portfolioId") String portfolioId,
            @Param("sequenceNo") String sequenceNo);

    /**
     * Find transaction by transaction ID
     */
    Optional<TransactionRecord> findByTransactionId(String transactionId);

    /**
     * Find all transactions by portfolio ID
     */
    List<TransactionRecord> findByPortfolioId(String portfolioId);

    /**
     * Find all transactions by portfolio ID with pagination
     */
    Page<TransactionRecord> findByPortfolioId(String portfolioId, Pageable pageable);

    /**
     * Find all transactions by portfolio ID and status
     */
    List<TransactionRecord> findByPortfolioIdAndStatus(String portfolioId, String status);

    /**
     * Find all transactions by transaction date
     */
    List<TransactionRecord> findByTransactionDate(LocalDate transactionDate);

    /**
     * Find transactions within date range
     */
    List<TransactionRecord> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by portfolio ID within date range
     */
    List<TransactionRecord> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by portfolio ID within date range with pagination
     */
    Page<TransactionRecord> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Find transactions by investment ID
     */
    List<TransactionRecord> findByInvestmentId(String investmentId);

    /**
     * Find transactions by transaction type
     */
    List<TransactionRecord> findByTransactionType(String transactionType);

    /**
     * Find transactions by portfolio ID and transaction type
     */
    List<TransactionRecord> findByPortfolioIdAndTransactionType(String portfolioId, String transactionType);

    /**
     * Find transactions by status
     */
    List<TransactionRecord> findByStatus(String status);

    /**
     * Find pending transactions
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.status = 'P' ORDER BY t.transactionDate, t.transactionTime")
    List<TransactionRecord> findPendingTransactions();

    /**
     * Find recent transactions (last 30 days)
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.transactionDate >= :startDate " +
            "ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<TransactionRecord> findRecentTransactions(@Param("startDate") LocalDate startDate);

    /**
     * Find recent transactions with pagination
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.transactionDate >= :startDate " +
            "ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    Page<TransactionRecord> findRecentTransactions(@Param("startDate") LocalDate startDate, Pageable pageable);

    /**
     * Get total amount by portfolio ID and transaction type
     */
    @Query("SELECT SUM(t.amount) FROM TransactionRecord t " +
            "WHERE t.portfolioId = :portfolioId AND t.transactionType = :type AND t.status = 'D'")
    BigDecimal getTotalAmountByPortfolioIdAndType(
            @Param("portfolioId") String portfolioId,
            @Param("type") String type);

    /**
     * Count transactions by portfolio ID
     */
    long countByPortfolioId(String portfolioId);

    /**
     * Count transactions by status
     */
    long countByStatus(String status);

    /**
     * Count transactions by portfolio ID and status
     */
    long countByPortfolioIdAndStatus(String portfolioId, String status);

    /**
     * Check if transaction exists by transaction ID
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Find buy transactions by portfolio ID
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId " +
            "AND t.transactionType = 'BU' AND t.status = 'D'")
    List<TransactionRecord> findBuyTransactions(@Param("portfolioId") String portfolioId);

    /**
     * Find sell transactions by portfolio ID
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId " +
            "AND t.transactionType = 'SL' AND t.status = 'D'")
    List<TransactionRecord> findSellTransactions(@Param("portfolioId") String portfolioId);

    /**
     * Find transactions by currency code
     */
    List<TransactionRecord> findByCurrencyCode(String currencyCode);
}
