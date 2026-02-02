package com.portfolio.repository;

import com.portfolio.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Transaction entity
 * Provides data access operations for financial transactions
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByPortfolioId(String portfolioId);

    Page<Transaction> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId, Pageable pageable);

    List<Transaction> findByPortfolioIdAndStatus(String portfolioId, Transaction.TransactionStatus status);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<Transaction> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.portfolioId = :portfolioId ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<Transaction> findTransactionHistory(@Param("portfolioId") String portfolioId);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'P' ORDER BY t.transactionDate, t.transactionTime")
    List<Transaction> findPendingTransactions();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.portfolioId = :portfolioId AND t.transactionType = :type")
    long countByPortfolioAndType(@Param("portfolioId") String portfolioId, 
                                  @Param("type") Transaction.TransactionType type);
}
