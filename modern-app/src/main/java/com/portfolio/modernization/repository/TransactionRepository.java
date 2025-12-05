package com.portfolio.modernization.repository;

import com.portfolio.modernization.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByPortfolioId(String portfolioId);

    List<Transaction> findByPortfolioIdAndTransactionDateBetween(String portfolioId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByTransactionType(Transaction.TransactionType transactionType);

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.portfolioId = :portfolioId ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<Transaction> findByPortfolioIdOrderByDateDesc(@Param("portfolioId") String portfolioId);

    @Query("SELECT t FROM Transaction t WHERE t.investmentId = :investmentId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByInvestmentIdAndDateRange(@Param("investmentId") String investmentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
