package com.portfolio.repository;

import com.portfolio.model.PositionHistory;
import com.portfolio.model.PositionHistoryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Position History entity.
 * Replaces: VSAM POSHIST file I/O and DB2 POSHIST table access
 * in HISTLD00.cbl, INQHIST.cbl.
 */
@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, PositionHistoryKey> {

    List<PositionHistory> findByPortfolioId(String portfolioId);

    List<PositionHistory> findByPortfolioIdAndHistoryDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    List<PositionHistory> findByHistoryDateBetween(LocalDate startDate, LocalDate endDate);

    List<PositionHistory> findByPortfolioIdOrderByHistoryDateDescHistoryTimeDesc(String portfolioId);
}
