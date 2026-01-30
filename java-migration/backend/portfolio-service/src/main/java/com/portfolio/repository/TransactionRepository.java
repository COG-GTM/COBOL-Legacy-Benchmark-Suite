package com.portfolio.repository;

import com.portfolio.model.entity.Transaction;
import com.portfolio.model.enums.TransactionStatus;
import com.portfolio.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByPortfolioId(String portfolioId);

    List<Transaction> findByPortfolioIdAndType(String portfolioId, TransactionType type);

    List<Transaction> findByPortfolioIdAndStatus(String portfolioId, TransactionStatus status);

    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<Transaction> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByStatus(TransactionStatus status);

    long countByPortfolioId(String portfolioId);

    long countByStatus(TransactionStatus status);
}
