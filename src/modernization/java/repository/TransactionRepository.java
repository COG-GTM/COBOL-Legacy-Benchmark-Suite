package com.portfolio.modernization.repository;

import com.portfolio.modernization.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Repository Interface
 * 
 * Provides data access operations for TransactionRecord entities.
 * Modernized from COBOL VSAM file operations in TRNVAL00.cbl and INQHIST.cbl
 * 
 * Original COBOL operations:
 * - READ TRANHIST (INQHIST.cbl)
 * - WRITE TRANHIST (TRNVAL00.cbl)
 * - START TRANHIST (for sequential reads)
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {

    /**
     * Find all transactions for a portfolio
     * Equivalent to COBOL: READ TRANHIST WITH KEY PORTFOLIO-ID
     * 
     * @param portfolioId the portfolio identifier
     * @return list of transactions for the portfolio
     */
    List<TransactionRecord> findByPortfolioId(String portfolioId);

    /**
     * Find transactions by portfolio ordered by date descending
     * 
     * @param portfolioId the portfolio identifier
     * @return list of transactions ordered by date
     */
    List<TransactionRecord> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId);

    /**
     * Find transactions by portfolio and date range
     * Equivalent to COBOL: START TRANHIST KEY >= date, READ NEXT
     * 
     * @param portfolioId the portfolio identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of transactions within the date range
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<TransactionRecord> findByPortfolioIdAndDateRange(
            @Param("portfolioId") String portfolioId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find transactions by type
     * 
     * @param transactionType the transaction type (BUY, SELL, TRANSFER, FEE)
     * @return list of transactions of the specified type
     */
    List<TransactionRecord> findByTransactionType(String transactionType);

    /**
     * Find transactions by status
     * 
     * @param status the transaction status (P, D, F, R)
     * @return list of transactions with the specified status
     */
    List<TransactionRecord> findByStatus(String status);

    /**
     * Find pending transactions
     * 
     * @return list of pending transactions
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.status = 'P' ORDER BY t.transactionDate ASC")
    List<TransactionRecord> findPendingTransactions();

    /**
     * Find failed transactions
     * 
     * @return list of failed transactions
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.status = 'F' ORDER BY t.processDate DESC")
    List<TransactionRecord> findFailedTransactions();

    /**
     * Find transactions by investment ID
     * 
     * @param investmentId the investment identifier
     * @return list of transactions for the investment
     */
    List<TransactionRecord> findByInvestmentId(String investmentId);

    /**
     * Find transactions by investment and date range
     * 
     * @param investmentId the investment identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of transactions for the investment within the date range
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.investmentId = :investmentId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<TransactionRecord> findByInvestmentIdAndDateRange(
            @Param("investmentId") String investmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find transactions by date
     * 
     * @param transactionDate the transaction date
     * @return list of transactions on the specified date
     */
    List<TransactionRecord> findByTransactionDate(LocalDate transactionDate);

    /**
     * Find transactions processed after a specific date
     * 
     * @param processDate the process date threshold
     * @return list of transactions processed after the date
     */
    List<TransactionRecord> findByProcessDateAfter(LocalDateTime processDate);

    /**
     * Calculate total transaction amount by portfolio and type
     * 
     * @param portfolioId the portfolio identifier
     * @param transactionType the transaction type
     * @return total amount
     */
    @Query("SELECT SUM(t.amount) FROM TransactionRecord t " +
           "WHERE t.portfolioId = :portfolioId AND t.transactionType = :transactionType AND t.status = 'D'")
    BigDecimal calculateTotalAmountByPortfolioAndType(
            @Param("portfolioId") String portfolioId,
            @Param("transactionType") String transactionType);

    /**
     * Calculate total units by portfolio and investment
     * 
     * @param portfolioId the portfolio identifier
     * @param investmentId the investment identifier
     * @return total units (buys - sells)
     */
    @Query("SELECT SUM(CASE WHEN t.transactionType IN ('BUY', 'BU') THEN t.units " +
           "WHEN t.transactionType IN ('SELL', 'SL') THEN -t.units ELSE 0 END) " +
           "FROM TransactionRecord t WHERE t.portfolioId = :portfolioId " +
           "AND t.investmentId = :investmentId AND t.status = 'D'")
    BigDecimal calculateNetUnitsByPortfolioAndInvestment(
            @Param("portfolioId") String portfolioId,
            @Param("investmentId") String investmentId);

    /**
     * Count transactions by status
     * 
     * @param status the transaction status
     * @return count of transactions with the specified status
     */
    long countByStatus(String status);

    /**
     * Count transactions by portfolio
     * 
     * @param portfolioId the portfolio identifier
     * @return count of transactions for the portfolio
     */
    long countByPortfolioId(String portfolioId);

    /**
     * Count transactions by type and date range
     * 
     * @param transactionType the transaction type
     * @param startDate start of date range
     * @param endDate end of date range
     * @return count of transactions
     */
    @Query("SELECT COUNT(t) FROM TransactionRecord t " +
           "WHERE t.transactionType = :transactionType " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    long countByTypeAndDateRange(
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find transactions migrated from VSAM
     * 
     * @return list of transactions that were migrated from VSAM
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.vsamMigrationDate IS NOT NULL")
    List<TransactionRecord> findMigratedTransactions();

    /**
     * Find transaction by VSAM record key
     * Used for migration verification and audit
     * 
     * @param vsamRecordKey the original VSAM record key
     * @return optional transaction if found
     */
    Optional<TransactionRecord> findByVsamRecordKey(String vsamRecordKey);

    /**
     * Get transaction summary by date
     * 
     * @param transactionDate the transaction date
     * @return array containing [count, total amount, buy count, sell count]
     */
    @Query("SELECT COUNT(t), SUM(t.amount), " +
           "SUM(CASE WHEN t.transactionType IN ('BUY', 'BU') THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.transactionType IN ('SELL', 'SL') THEN 1 ELSE 0 END) " +
           "FROM TransactionRecord t WHERE t.transactionDate = :transactionDate AND t.status = 'D'")
    Object[] getTransactionSummaryByDate(@Param("transactionDate") LocalDate transactionDate);

    /**
     * Find recent transactions
     * 
     * @param days number of days to look back
     * @return list of recent transactions
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.transactionDate >= :startDate " +
           "ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<TransactionRecord> findRecentTransactions(@Param("startDate") LocalDate startDate);

    /**
     * Find transactions by currency
     * 
     * @param currencyCode the 3-character currency code
     * @return list of transactions in the specified currency
     */
    List<TransactionRecord> findByCurrencyCode(String currencyCode);

    /**
     * Find transactions by process user
     * 
     * @param processUser the user who processed the transaction
     * @return list of transactions processed by the user
     */
    List<TransactionRecord> findByProcessUser(String processUser);

    /**
     * Check if transaction exists for portfolio on date
     * 
     * @param portfolioId the portfolio identifier
     * @param transactionDate the transaction date
     * @return true if transaction exists
     */
    boolean existsByPortfolioIdAndTransactionDate(String portfolioId, LocalDate transactionDate);

    /**
     * Find buy transactions for a portfolio
     * 
     * @param portfolioId the portfolio identifier
     * @return list of buy transactions
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId " +
           "AND t.transactionType IN ('BUY', 'BU') AND t.status = 'D' " +
           "ORDER BY t.transactionDate DESC")
    List<TransactionRecord> findBuyTransactionsByPortfolio(@Param("portfolioId") String portfolioId);

    /**
     * Find sell transactions for a portfolio
     * 
     * @param portfolioId the portfolio identifier
     * @return list of sell transactions
     */
    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId " +
           "AND t.transactionType IN ('SELL', 'SL') AND t.status = 'D' " +
           "ORDER BY t.transactionDate DESC")
    List<TransactionRecord> findSellTransactionsByPortfolio(@Param("portfolioId") String portfolioId);
}
