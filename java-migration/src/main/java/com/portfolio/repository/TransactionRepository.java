package com.portfolio.repository;

import com.portfolio.domain.Transaction;
import com.portfolio.domain.TransactionStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
  List<Transaction> findByStatusOrderByTransactionDateAscTransactionTimeAsc(
      TransactionStatus status);

  List<Transaction> findByStatus(TransactionStatus status);

  Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

  List<Transaction> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId);

  long countByPortfolioIdAndTransactionDate(String portfolioId, LocalDate transactionDate);
}
