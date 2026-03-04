package com.cobolbenchmark.db;

import com.cobolbenchmark.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Transaction Repository - replaces VSAM transaction file operations.
 * Provides CRUD access to TRANSACTION_HISTORY table.
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {

    List<TransactionRecord> findByPortfolioId(String portfolioId);

    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId ORDER BY t.transactionDate DESC")
    List<TransactionRecord> findByPortfolioIdOrderByDateDesc(@Param("portfolioId") String portfolioId);

    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId AND t.transactionType = :type")
    List<TransactionRecord> findByPortfolioIdAndType(
            @Param("portfolioId") String portfolioId,
            @Param("type") String type);

    @Query("SELECT t FROM TransactionRecord t WHERE t.portfolioId = :portfolioId AND t.status = :status")
    List<TransactionRecord> findByPortfolioIdAndStatus(
            @Param("portfolioId") String portfolioId,
            @Param("status") String status);
}
