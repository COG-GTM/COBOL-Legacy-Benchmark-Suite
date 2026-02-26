package com.portfolio.support;

import com.portfolio.model.HistoryRecord;
import com.portfolio.model.HistoryRecordKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Transaction History VSAM table.
 * Replaces VSAM transaction history cluster access.
 */
@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, HistoryRecordKey> {

    List<HistoryRecord> findByPortfolioId(String portfolioId);

    List<HistoryRecord> findByPortfolioIdAndTxnDate(String portfolioId, LocalDate txnDate);

    long countByPortfolioId(String portfolioId);
}
