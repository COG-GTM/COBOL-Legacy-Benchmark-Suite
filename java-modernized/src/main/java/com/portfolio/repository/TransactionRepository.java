package com.portfolio.repository;

import com.portfolio.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Transaction entity.
 * Mirrors VSAM TRANHIST file access patterns from PORTTRAN.cbl.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByPortfolioId(String portfolioId);

    List<Transaction> findByPortfolioIdAndTransactionType(String portfolioId, String transactionType);

    List<Transaction> findByPortfolioIdAndStatus(String portfolioId, String status);
}
