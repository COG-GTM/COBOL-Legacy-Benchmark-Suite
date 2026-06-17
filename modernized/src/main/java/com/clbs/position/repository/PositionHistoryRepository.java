package com.clbs.position.repository;

import com.clbs.position.entity.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data repository for {@code POSHIST} &mdash; the modern equivalent of the
 * embedded DB2 SQL the COBOL programs executed against the position-history
 * table. The COBOL DB2 access was {@code EXEC SQL INSERT INTO POSHIST ...}
 * (history load) and read cursors for reporting; those become repository
 * {@code save} calls and the derived/JPQL queries below.
 */
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    /**
     * Equivalent of the DB2 reporting query
     * {@code SELECT ... FROM POSHIST WHERE PORTFOLIO_ID = :p AND TRANS_DATE = :d}.
     */
    List<PositionHistory> findByPortfolioIdAndTransDate(String portfolioId, LocalDate transDate);

    List<PositionHistory> findBySecurityId(String securityId);

    /**
     * Realized P&amp;L roll-up for a portfolio &mdash; conversion of a DB2
     * aggregate query ({@code SELECT SUM(GAIN_LOSS) FROM POSHIST WHERE
     * PORTFOLIO_ID = :portfolioId}) to JPQL.
     */
    @Query("SELECT COALESCE(SUM(h.gainLoss), 0) FROM PositionHistory h "
            + "WHERE h.portfolioId = :portfolioId")
    java.math.BigDecimal sumRealizedGainLoss(@Param("portfolioId") String portfolioId);
}
