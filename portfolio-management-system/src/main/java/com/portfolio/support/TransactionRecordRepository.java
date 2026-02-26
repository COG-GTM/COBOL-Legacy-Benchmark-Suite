package com.portfolio.support;

import com.portfolio.model.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Transaction History table.
 * Replaces DB2 cursor-based access in INQHIST / CURSMGR.
 * Spring Data Pageable replaces CURSMGR array fetch/pagination.
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, String> {

    Page<TransactionRecord> findByPortfolioIdOrderByTransactionDateDesc(String portfolioId, Pageable pageable);

    List<TransactionRecord> findByPortfolioIdAndTransactionDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    long countByPortfolioId(String portfolioId);

    long countByStatus(String status);
}
