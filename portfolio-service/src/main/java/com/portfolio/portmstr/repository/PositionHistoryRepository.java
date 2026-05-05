package com.portfolio.portmstr.repository;

import com.portfolio.portmstr.model.PositionHistory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Position History records.
 * Replaces COBOL DB2 operations on POSHIST table.
 */
@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    List<PositionHistory> findByPortfolioId(String portfolioId);

    List<PositionHistory> findBySecurityIdAndTransDateBetween(
            String securityId, LocalDate startDate, LocalDate endDate);
}
