package com.portfolio.repository;

import com.portfolio.model.TransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Transaction History entity.
 * Replaces: DB2 TRANSACTION_HISTORY SQL and VSAM TRANHIST file I/O
 * in TRNVAL00.cbl, HISTLD00.cbl, INQHIST.cbl, RPTAUD00.cbl.
 */
@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {

    List<TransactionHistory> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId);

    Page<TransactionHistory> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId, Pageable pageable);

    List<TransactionHistory> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<TransactionHistory> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    List<TransactionHistory> findByStatus(String status);

    long countByPortfolioIdAndStatus(String portfolioId, String status);
}
