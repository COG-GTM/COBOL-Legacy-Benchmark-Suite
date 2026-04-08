package com.portfolio.repository;

import com.portfolio.model.TransactionRecord;
import com.portfolio.model.TransactionRecordKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for TransactionRecord entities.
 * Replaces VSAM file access for transaction records in the original COBOL system.
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, TransactionRecordKey> {

    /**
     * Find transactions for a portfolio within a date range.
     * Used by P400-HISTORY-INQUIRY in INQONLN.cbl.
     */
    List<TransactionRecord> findByPortfolioIdAndTransDateBetween(
            String portfolioId, LocalDate from, LocalDate to);
}
