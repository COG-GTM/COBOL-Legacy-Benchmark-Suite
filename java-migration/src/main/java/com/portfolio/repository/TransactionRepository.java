package com.portfolio.repository;

import com.portfolio.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Transaction repository - replaces VSAM TRANHIST KSDS file access.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByPortfolioId(String portfolioId);

    List<Transaction> findByPortfolioIdAndTransactionDate(String portfolioId, LocalDate date);

    Page<Transaction> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId, Pageable pageable);

    List<Transaction> findByPortfolioIdAndStatus(String portfolioId, String status);

    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<Transaction> findByStatus(String status);
}
