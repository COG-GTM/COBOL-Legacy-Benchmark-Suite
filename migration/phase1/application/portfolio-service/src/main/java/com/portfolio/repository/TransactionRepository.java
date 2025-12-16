package com.portfolio.repository;

import com.portfolio.entity.Transaction;
import com.portfolio.entity.Transaction.TransactionStatus;
import com.portfolio.entity.Transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Transaction entity.
 * Replaces VSAM TRANHIST file access operations.
 * 
 * @see src/copybook/common/TRNREC.cpy
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByPortfolioId(String portfolioId, Pageable pageable);

    List<Transaction> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    Page<Transaction> findByPortfolioIdAndTransactionType(
            String portfolioId, TransactionType transactionType, Pageable pageable);

    List<Transaction> findByStatus(TransactionStatus status);

    Page<Transaction> findByTransactionDateBetween(
            LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.portfolioId = :portfolioId " +
           "AND t.investmentId = :investmentId ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<Transaction> findByPortfolioAndInvestment(
            @Param("portfolioId") String portfolioId,
            @Param("investmentId") String investmentId);

    @Query("SELECT SUM(t.totalAmount) FROM Transaction t WHERE t.portfolioId = :portfolioId " +
           "AND t.transactionType = :type AND t.status = 'COMPLETED'")
    BigDecimal sumAmountByPortfolioAndType(
            @Param("portfolioId") String portfolioId,
            @Param("type") TransactionType type);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate = :date AND t.status = :status")
    long countByDateAndStatus(@Param("date") LocalDate date, @Param("status") TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' ORDER BY t.transactionDate, t.transactionTime")
    List<Transaction> findPendingTransactions();

    boolean existsByTransactionId(String transactionId);
}
