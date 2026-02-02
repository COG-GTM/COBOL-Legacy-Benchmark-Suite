package com.portfolio.repository;

import com.portfolio.domain.HistoryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for HistoryRecord entity
 * Provides data access operations for audit history
 */
@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, Long> {

    List<HistoryRecord> findByPortfolioId(String portfolioId);

    Page<HistoryRecord> findByPortfolioIdOrderByHistoryDateDescHistoryTimeDesc(
            String portfolioId, Pageable pageable);

    List<HistoryRecord> findByRecordType(HistoryRecord.RecordType recordType);

    List<HistoryRecord> findByActionCode(HistoryRecord.ActionCode actionCode);

    List<HistoryRecord> findByHistoryDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT h FROM HistoryRecord h WHERE h.portfolioId = :portfolioId " +
           "ORDER BY h.historyDate DESC, h.historyTime DESC")
    List<HistoryRecord> findHistoryByPortfolio(@Param("portfolioId") String portfolioId);

    @Query("SELECT h FROM HistoryRecord h WHERE h.processUser = :userId " +
           "ORDER BY h.processDate DESC")
    List<HistoryRecord> findByUser(@Param("userId") String userId);
}
