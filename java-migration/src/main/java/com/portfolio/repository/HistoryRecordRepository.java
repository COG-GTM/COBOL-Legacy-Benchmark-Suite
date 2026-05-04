package com.portfolio.repository;

import com.portfolio.domain.HistoryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * History Record repository - replaces COBOL HISTREC file access.
 */
@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {

    List<HistoryRecord> findByPortfolioId(String portfolioId);

    Page<HistoryRecord> findByPortfolioIdOrderByHistoryDateDescHistoryTimeDesc(
            String portfolioId, Pageable pageable);

    List<HistoryRecord> findByPortfolioIdAndHistoryDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    List<HistoryRecord> findByRecordType(String recordType);
}
