package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.TransactionHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link TransactionHistory}. */
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {
  /** Transactions of one portfolio in chronological order (index {@code IDX_TRANS_HIST_PORT}). */
  List<TransactionHistory> findByPortfolioIdOrderByTransactionDateAscTransactionTimeAsc(
      String portfolioId);
}
