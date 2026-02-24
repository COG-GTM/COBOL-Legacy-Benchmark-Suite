package com.investment.portfolio.repository;

import com.investment.portfolio.entity.TransactionHistory;
import com.investment.portfolio.entity.TransactionHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository for the TransactionHistory (POSHIST) entity.
 *
 * Uses the composite key {@link TransactionHistoryId} matching the DB2
 * POSHIST primary key (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME).
 */
@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, TransactionHistoryId> {

    /**
     * Find all history records for a given account.
     */
    List<TransactionHistory> findByIdAccountNo(String accountNo);

    /**
     * Find history records by security ID and date range.
     * Replaces DB2 POSHIST_IX1 index access pattern.
     */
    List<TransactionHistory> findBySecurityIdAndIdTransDateBetween(
            String securityId, LocalDate startDate, LocalDate endDate);

    /**
     * Find history records by process date and program.
     * Replaces DB2 POSHIST_IX2 index access pattern.
     */
    List<TransactionHistory> findByProcessDateAndProgramId(LocalDate processDate, String programId);

    /**
     * Find all history records for a portfolio.
     */
    List<TransactionHistory> findByIdPortfolioId(String portfolioId);
}
