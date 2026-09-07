package com.portfolio.repository;

import com.portfolio.domain.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
  List<Transaction> findByStatusOrderByTransactionDateAscTransactionTimeAsc(
      TransactionStatus status);

  List<Transaction> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId);

  long countByPortfolioIdAndTransactionDate(String portfolioId, LocalDate transactionDate);
}
