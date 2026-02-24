package com.investment.portfolio.repository;

import com.investment.portfolio.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository for the Transaction entity.
 *
 * Provides access patterns matching the original COBOL/DB2 index-based queries
 * on the TRANSACTION_HISTORY table.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Find all transactions for a given portfolio, ordered by date.
     * Replaces DB2 IDX_TRANS_HIST_PORT index access pattern.
     */
    List<Transaction> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId);

    /**
     * Find transactions for a portfolio within a date range.
     * Supports the COBOL INQHIST date-range query pattern.
     */
    List<Transaction> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all transactions on a given date.
     * Uses the DB2 IDX_TRANS_HIST_DATE index pattern.
     */
    List<Transaction> findByTransactionDate(LocalDate transactionDate);

    /**
     * Find transactions by type and status.
     * Supports batch processing filter patterns (BU/SL/TR/FE + P/F/R).
     */
    List<Transaction> findByTransactionTypeAndStatus(String transactionType, String status);

    /**
     * Find transactions for a portfolio by type.
     */
    @Query("SELECT t FROM Transaction t WHERE t.portfolio.portfolioId = :portfolioId " +
           "AND t.transactionType = :type ORDER BY t.transactionDate DESC")
    List<Transaction> findByPortfolioAndType(
            @Param("portfolioId") String portfolioId,
            @Param("type") String type);
}
