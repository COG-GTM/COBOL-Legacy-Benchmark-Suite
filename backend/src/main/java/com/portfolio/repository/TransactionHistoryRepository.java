package com.portfolio.repository;

import com.portfolio.entity.TransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository for TransactionHistory entity.
 * Replaces INQHIST.cbl cursor-based array fetch (HISTORY_CURSOR)
 * with standard Spring Data pagination (Pageable).
 */
@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {

    Page<TransactionHistory> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId, Pageable pageable);

    List<TransactionHistory> findByPortfolioId(String portfolioId);

    List<TransactionHistory> findByStatus(String status);

    @Query("SELECT th FROM TransactionHistory th WHERE th.portfolioId = :portfolioId " +
            "AND th.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY th.transactionDate DESC")
    Page<TransactionHistory> findByPortfolioIdAndDateRange(
            @Param("portfolioId") String portfolioId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    List<TransactionHistory> findByPortfolioIdAndTransactionType(String portfolioId, String transactionType);

    long countByStatus(String status);
}
