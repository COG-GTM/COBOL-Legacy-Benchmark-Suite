package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.HistoryRecord;
import com.clbs.portfolio.model.HistoryRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for HistoryRecord entities.
 * Replaces VSAM access patterns for HISTREC file.
 */
@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, HistoryRecordId> {

    List<HistoryRecord> findByPortfolioIdAndHistDateBetween(String portfolioId, String startDate, String endDate);
}
