package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    List<TransactionRecord> findByPortfolioId(String portfolioId);

    List<TransactionRecord> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<TransactionRecord> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId NOT IN " +
           "(SELECT p.portfolioId FROM Portfolio p)")
    List<TransactionRecord> findOrphanedTransactions();

    @Query("SELECT t FROM TransactionRecord t WHERE t.transactionDate < :cutoffDate")
    List<TransactionRecord> findOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    long countByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
}
