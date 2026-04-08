package com.portfolio.repository;

import com.portfolio.model.PositionHistory;
import com.portfolio.model.PositionHistoryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for PositionHistory entities.
 * Replaces embedded SQL / DB2 cursor access used in INQHIST.cbl
 * and the HISTLD00 batch program.
 *
 * In the COBOL system, INQHIST opened a DB2 cursor over the POSHIST table
 * with a WHERE clause on PORTFOLIO_ID and TRANS_DATE range, then fetched
 * rows one at a time. Spring Data JPA derived queries replace all of that.
 */
@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, PositionHistoryKey> {

    /**
     * Lookup history by portfolio ID and date range.
     * Replaces the DB2 cursor in INQHIST.cbl.
     */
    List<PositionHistory> findByPortfolioIdAndTransDateBetween(
            String portfolioId, LocalDate from, LocalDate to);

    /**
     * Lookup all history for a portfolio (no date filter).
     */
    List<PositionHistory> findByPortfolioId(String portfolioId);
}
