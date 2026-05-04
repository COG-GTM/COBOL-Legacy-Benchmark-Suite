package com.portfolio.repository;

import com.portfolio.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {

    List<TransactionRecord> findByPortfolioIdOrderByTransactionDateDescTransactionTimeDesc(String portfolioId);

    List<TransactionRecord> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    List<TransactionRecord> findByStatus(String status);

    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId ORDER BY t.transactionDate DESC, t.transactionTime DESC")
    List<TransactionRecord> findHistoryByPortfolioId(@Param("portfolioId") String portfolioId);

    List<TransactionRecord> findByTransactionType(String transactionType);

    long countByStatus(String status);
}
