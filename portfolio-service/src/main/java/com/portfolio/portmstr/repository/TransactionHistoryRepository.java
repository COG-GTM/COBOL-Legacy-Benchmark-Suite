package com.portfolio.portmstr.repository;

import com.portfolio.portmstr.model.TransactionHistory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Transaction History records.
 * Replaces COBOL VSAM KSDS file I/O on the TRANHIST file and
 * DB2 queries on TRANSACTION_HISTORY table.
 */
@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {

    List<TransactionHistory> findByPortfolioId(String portfolioId);

    List<TransactionHistory> findByPortfolioIdAndTransactionDate(String portfolioId, LocalDate date);

    List<TransactionHistory> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
}
